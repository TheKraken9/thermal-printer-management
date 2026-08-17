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

        // ── Entreprise ────────────────────────────────────────────────────
        appendCenteredByDash(sb, d.boutiqueName);
        if (isNotEmpty(d.boutiqueAddress)) appendCenteredByDash(sb, d.boutiqueAddress);
        if (isNotEmpty(d.boutiquePhone))   sb.append(center("Tel: " + ascii(d.boutiquePhone))).append("\n");
        if (isNotEmpty(d.boutiqueNIF))     sb.append(center("NIF: " + ascii(d.boutiqueNIF))).append("\n");
        if (isNotEmpty(d.boutiqueStat))    sb.append(center("STAT: " + ascii(d.boutiqueStat))).append("\n");
        sb.append(separator('=')).append("\n");

        // ── Entête document ───────────────────────────────────────────────
        String dateHeure = ascii(nvl(d.date));
        if (isNotEmpty(d.time)) dateHeure += " " + ascii(d.time);
        sb.append(lineLeftRight("No: " + ascii(nvl(d.numero)), dateHeure)).append("\n");
        if (isNotEmpty(d.deliveredByName)) {
            sb.append("Livre par: ").append(ascii(d.deliveredByName)).append("\n");
        }
        sb.append(separator('=')).append("\n");

        // ── Destinataire ──────────────────────────────────────────────────
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

        // ── Produits livrés (sans prix) ───────────────────────────────────
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

        // ── DEBUG : apercu dans la console de l'agent ─────────────────────
        System.out.println("\n========== BON DE LIVRAISON " + nvl(d.numero) + " ==========");
        System.out.println(ticketText);
        System.out.println("========== FIN BON DE LIVRAISON ==========\n");

        // Poste de dev sans imprimante : on ecrit le bon dans un fichier.
        if (service == null) {
            java.nio.file.Path out = java.nio.file.Paths.get(
                    System.getProperty("java.io.tmpdir"),
                    "bon-livraison-" + System.currentTimeMillis() + ".txt");
            java.nio.file.Files.write(out, ticketText.getBytes(StandardCharsets.UTF_8));
            System.out.println("Aucune imprimante detectee (poste de dev). Bon de livraison ecrit dans : " + out);
            return;
        }

        System.out.println("Imprimante : " + service.getName());
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
                case 'à': case 'â': case 'ä': case 'á': sb.append('a'); break;
                case 'è': case 'ê': case 'ë': case 'é': sb.append('e'); break;
                case 'ì': case 'î': case 'ï': case 'í': sb.append('i'); break;
                case 'ò': case 'ô': case 'ö': case 'ó': sb.append('o'); break;
                case 'ù': case 'û': case 'ü': case 'ú': sb.append('u'); break;
                case 'ç': sb.append('c'); break;
                case 'ñ': sb.append('n'); break;
                case 'À': case 'Â': case 'Ä': case 'Á': sb.append('A'); break;
                case 'È': case 'Ê': case 'Ë': case 'É': sb.append('E'); break;
                case 'Ì': case 'Î': case 'Ï': case 'Í': sb.append('I'); break;
                case 'Ò': case 'Ô': case 'Ö': case 'Ó': sb.append('O'); break;
                case 'Ù': case 'Û': case 'Ü': case 'Ú': sb.append('U'); break;
                case 'Ç': sb.append('C'); break;
                case 'Ñ': sb.append('N'); break;
                case '°': sb.append('o'); break;
                case '«': case '»': sb.append('"'); break;
                case '‘': case '’': sb.append('\''); break;
                case '“': case '”': sb.append('"'); break;
                case '–': case '—': sb.append('-'); break;
                case ' ': case ' ': sb.append(' '); break;
                default:
                    if (ch < 0x80) sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }
}
