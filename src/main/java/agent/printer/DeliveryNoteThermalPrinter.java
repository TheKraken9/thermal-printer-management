package agent.printer;

import agent.dto.DeliveryLineDTO;
import agent.dto.DeliveryNoteDTO;

import javax.print.*;
import java.nio.charset.StandardCharsets;

/**
 * Impression thermique d'un BON DE LIVRAISON.
 * Même mécanique que les autres imprimantes : construction du texte (48 col.)
 * puis envoi ESC/POS ; repli fichier si aucune imprimante (poste de dev).
 */
public class DeliveryNoteThermalPrinter {

    private static final int LINE_WIDTH = 48;

    public void print(DeliveryNoteDTO d) throws Exception {
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();

        StringBuilder sb = new StringBuilder();

        sb.append(separator('=')).append("\n");
        sb.append(center("*** BON DE LIVRAISON ***")).append("\n");
        sb.append(separator('=')).append("\n");

        if (isNotEmpty(d.saleBoutiqueName)) {
            sb.append(center("MAGASIN D'ORIGINE")).append("\n");
            sb.append(center(ascii(d.saleBoutiqueName))).append("\n");
            sb.append(separator('=')).append("\n");
        }

        appendCenteredByDash(sb, d.boutiqueName);
        if (isNotEmpty(d.boutiqueAddress)) appendCenteredByDash(sb, d.boutiqueAddress);
        if (isNotEmpty(d.boutiquePhone))   sb.append(center("Tel: " + ascii(d.boutiquePhone))).append("\n");
        if (isNotEmpty(d.boutiqueNIF))     sb.append(center("NIF: " + ascii(d.boutiqueNIF))).append("\n");
        if (isNotEmpty(d.boutiqueStat))    sb.append(center("STAT: " + ascii(d.boutiqueStat))).append("\n");
        sb.append(separator('=')).append("\n");

        String dateHeure = ascii(nvl(d.date));
        if (isNotEmpty(d.time)) dateHeure += " " + ascii(d.time);
        sb.append(lineLeftRight("No: " + ascii(nvl(d.numero)), dateHeure)).append("\n");
        if (isNotEmpty(d.deliveredByName)) {
            sb.append("Livre par: ").append(ascii(d.deliveredByName)).append("\n");
        }
        sb.append(separator('=')).append("\n");

        sb.append(center("LIVRER A")).append("\n");
        sb.append(separator('-')).append("\n");
        if (isNotEmpty(d.clientName))  sb.append(ascii(d.clientName)).append("\n");
        if (isNotEmpty(d.clientPhone)) sb.append("Tel: ").append(ascii(d.clientPhone)).append("\n");
        if (isNotEmpty(d.deliveryAddress)) {
            appendMultiline(sb, "Adr: ", ascii(d.deliveryAddress), LINE_WIDTH - 5);
        }
        if (isNotEmpty(d.instructions)) {
            appendMultiline(sb, "Instr: ", ascii(d.instructions), LINE_WIDTH - 7);
        }
        if (isNotEmpty(d.note)) {
            appendMultiline(sb, "Note: ", ascii(d.note), LINE_WIDTH - 6);
        }
        if (isNotEmpty(d.moreInfo)) {
            appendMultiline(sb, "Infos: ", ascii(d.moreInfo), LINE_WIDTH - 7);
        }
        sb.append(separator('=')).append("\n");

        sb.append(lineLeftRight("Article", "Qte")).append("\n");
        sb.append(separator('-')).append("\n");

        if (d.lignes != null) {
            String curGroupTitle = null;
            String curGroupSub = null;
            int i = 1;
            for (DeliveryLineDTO l : d.lignes) {
                // En-tetes de groupe (piece / sous-titre) au changement.
                if (isNotEmpty(l.groupTitle) && !l.groupTitle.equals(curGroupTitle)) {
                    curGroupTitle = l.groupTitle;
                    curGroupSub = null;
                    sb.append(center(">> " + ascii(l.groupTitle).toUpperCase() + " <<")).append("\n");
                }
                if (isNotEmpty(l.groupSubtitle) && !l.groupSubtitle.equals(curGroupSub)) {
                    curGroupSub = l.groupSubtitle;
                    sb.append("* ").append(ascii(l.groupSubtitle)).append("\n");
                }

                String qteStr = formatQuantity(l.quantite);
                if (isNotEmpty(l.unit)) qteStr += " " + ascii(l.unit);

                String designation = i + ". " + ascii(nvl(l.designation));
                appendMultiline(sb, "", designation, LINE_WIDTH);
                sb.append(lineLeftRight("  Qte:", qteStr)).append("\n");

                if (isNotEmpty(l.reference)) {
                    sb.append("  Ref: ").append(ascii(l.reference)).append("\n");
                }
                if (isNotEmpty(l.note)) {
                    appendMultiline(sb, "  N.B: ", ascii(l.note), LINE_WIDTH - 7);
                }
                i++;
            }
        }

        sb.append(separator('=')).append("\n");

        // ── Montants pour le livreur (encaissement à la livraison) ────────
        // Seulement pour une vente client : un bon de tournée entre magasins
        // n'envoie aucun montant, et n'a rien à encaisser.
        boolean hasAmounts = d.totalAmount != null || d.amountPaid != null || d.remainingAmount != null;
        if (hasAmounts) {
            sb.append(center("A ENCAISSER A LA LIVRAISON")).append("\n");
            sb.append(separator('-')).append("\n");
            if (d.totalAmount != null) {
                sb.append(lineLeftRight("Total vente:", formatPrice(d.totalAmount) + " Ar")).append("\n");
            }
            if (d.deliveryFee != null && d.deliveryFee > 0) {
                sb.append(lineLeftRight("  dont frais livraison:", formatPrice(d.deliveryFee) + " Ar")).append("\n");
            }
            if (d.amountPaid != null && d.amountPaid > 0) {
                sb.append(lineLeftRight("Deja paye:", formatPrice(d.amountPaid) + " Ar")).append("\n");
            }
            long reste = d.remainingAmount != null
                    ? d.remainingAmount
                    : (d.totalAmount != null ? d.totalAmount : 0L) - (d.amountPaid != null ? d.amountPaid : 0L);
            sb.append(separator('-')).append("\n");
            if (reste > 0) {
                sb.append(lineLeftRight(">> RESTE A PAYER:", formatPrice(reste) + " Ar")).append("\n");
            } else {
                sb.append(center("** DEJA REGLE - RIEN A ENCAISSER **")).append("\n");
            }
            sb.append(separator('=')).append("\n");
        }

        // ── Signatures ────────────────────────────────────────────────────
        sb.append("\n");
        sb.append("Livre par (nom & signature) :").append("\n\n\n");
        sb.append("Recu par (nom & signature) :").append("\n\n\n");

        if (isNotEmpty(d.verifyCode)) {
            sb.append(separator('-')).append("\n");
            sb.append(center("Code d'authenticite")).append("\n");
            sb.append(center(ascii(d.verifyCode))).append("\n");
        }

        sb.append(separator('=')).append("\n");
        sb.append(center("Merci de verifier la marchandise")).append("\n");
        sb.append(center("a la reception.")).append("\n");
        sb.append("\n\n\n\n");

        String ticketText = sb.toString();


        // Poste de dev sans imprimante : on ecrit le bon dans un fichier.
        if (service == null) {
            java.nio.file.Path out = java.nio.file.Paths.get(
                    System.getProperty("java.io.tmpdir"),
                    "bon-livraison-" + System.currentTimeMillis() + ".txt");
            java.nio.file.Files.write(out, ticketText.getBytes(StandardCharsets.UTF_8));
            return;
        }

        WindowsPrinter.sendToPrinter(service, ticketText);
    }

    // ── Helpers (mise en page 48 colonnes) ────────────────────────────────

    private String formatPrice(long amount) {
        String raw = Long.toString(Math.abs(amount));
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = raw.length() - 1; i >= 0; i--) {
            sb.append(raw.charAt(i));
            count++;
            if (count % 3 == 0 && i > 0) sb.append(' ');
        }
        String result = sb.reverse().toString();
        return amount < 0 ? "-" + result : result;
    }

    private String formatQuantity(double qty) {
        if (qty == Math.floor(qty)) return String.valueOf((long) qty);
        return String.valueOf(qty);
    }

    private String lineLeftRight(String left, String right) {
        left  = nvl(left);
        right = nvl(right);
        int spaces = LINE_WIDTH - left.length() - right.length();
        return spaces > 0 ? left + " ".repeat(spaces) + right : left + " " + right;
    }

    private String center(String text) {
        text = nvl(text);
        if (text.length() >= LINE_WIDTH) return text.substring(0, LINE_WIDTH);
        int pad = (LINE_WIDTH - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private String separator(char c) {
        return String.valueOf(c).repeat(LINE_WIDTH);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private boolean isNotEmpty(String s) { return s != null && !s.trim().isEmpty(); }

    private String wrapText(String text, int width) {
        if (text == null) return "";
        text = text.trim();
        if (text.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.length() > width) {
                if (line.length() > 0) {
                    if (result.length() > 0) result.append("\n");
                    result.append(line);
                    line = new StringBuilder();
                }
                int start = 0;
                while (start < word.length()) {
                    int end = Math.min(start + width, word.length());
                    if (result.length() > 0) result.append("\n");
                    result.append(word, start, end);
                    start = end;
                }
            } else if (line.length() + word.length() + 1 > width) {
                if (result.length() > 0) result.append("\n");
                result.append(line);
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            }
        }
        if (line.length() > 0) {
            if (result.length() > 0) result.append("\n");
            result.append(line);
        }
        return result.toString();
    }

    private void appendMultiline(StringBuilder sb, String prefix, String text, int width) {
        String indent = " ".repeat(prefix.length());
        String[] rawLines = (text == null ? "" : text).split("\r?\n", -1);
        boolean first = true;
        for (String raw : rawLines) {
            String wrapped = wrapText(raw, width);
            String[] parts = wrapped.isEmpty() ? new String[]{""} : wrapped.split("\n");
            for (String part : parts) {
                sb.append(first ? prefix : indent).append(part).append("\n");
                first = false;
            }
        }
    }

    private void appendCenteredByDash(StringBuilder sb, String text) {
        for (String part : ascii(nvl(text)).split(" - ")) {
            if (!part.trim().isEmpty()) sb.append(center(part.trim())).append("\n");
        }
    }

    private String ascii(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '\u00e0': case '\u00e2': case '\u00e4': case '\u00e1': sb.append('a'); break;
                case '\u00e8': case '\u00ea': case '\u00eb': case '\u00e9': sb.append('e'); break;
                case '\u00ec': case '\u00ee': case '\u00ef': case '\u00ed': sb.append('i'); break;
                case '\u00f2': case '\u00f4': case '\u00f6': case '\u00f3': sb.append('o'); break;
                case '\u00f9': case '\u00fb': case '\u00fc': case '\u00fa': sb.append('u'); break;
                case '\u00e7': sb.append('c'); break;
                case '\u00f1': sb.append('n'); break;
                case '\u00c0': case '\u00c2': case '\u00c4': case '\u00c1': sb.append('A'); break;
                case '\u00c8': case '\u00ca': case '\u00cb': case '\u00c9': sb.append('E'); break;
                case '\u00cc': case '\u00ce': case '\u00cf': case '\u00cd': sb.append('I'); break;
                case '\u00d2': case '\u00d4': case '\u00d6': case '\u00d3': sb.append('O'); break;
                case '\u00d9': case '\u00db': case '\u00dc': case '\u00da': sb.append('U'); break;
                case '\u00c7': sb.append('C'); break;
                case '\u00d1': sb.append('N'); break;
                case '\u00b0': sb.append('o'); break;
                case '\u00ab': case '\u00bb': sb.append('"'); break;
                case '\u2018': case '\u2019': sb.append('\''); break;
                case '\u201c': case '\u201d': sb.append('"'); break;
                case '\u2013': case '\u2014': sb.append('-'); break;
                case '\u00a0': case '\u202f': sb.append(' '); break;
                default:
                    if (ch < 0x80) sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }
}
