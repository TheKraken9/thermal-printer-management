package agent.printer;

import agent.dto.ReceiptDTO;
import agent.dto.ReceiptLineDTO;

import javax.print.*;
import java.nio.charset.StandardCharsets;

public class WindowsPrinter {

    private static final int LINE_WIDTH = 48;

    public void print(ReceiptDTO r) throws Exception {
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        if (service == null) throw new RuntimeException("Aucune imprimante Windows");

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(center(ascii(r.boutiqueName))).append("\n");
        if (isNotEmpty(r.boutiqueAddress)) {
            sb.append(center(ascii(r.boutiqueAddress))).append("\n");
        }
        if (isNotEmpty(r.boutiquePhone)) {
            sb.append(center("Tel: " + ascii(r.boutiquePhone))).append("\n");
        }
        if (isNotEmpty(r.boutiqueNIF)) {
            sb.append(center("NIF: " + ascii(r.boutiqueNIF))).append("\n");
        }
        if (isNotEmpty(r.boutiqueStat)) {
            sb.append(center("STAT: " + ascii(r.boutiqueStat))).append("\n");
        }
        sb.append(center(r.type != null && r.type.equals("FACTURE") ? "FACTURE" : "RECU DE VENTE")).append("\n");

        sb.append(separator('=')).append("\n");

        sb.append(lineLeftRight("No: " + ascii(nvl(r.numero)), ascii(nvl(r.date)))).append("\n");
        if (isNotEmpty(r.caissier)) {
            sb.append("Caissiere: ").append(ascii(r.caissier)).append("\n");
        }

        if (isNotEmpty(r.clientName)) {
            sb.append(separator('=')).append("\n");
            sb.append("Client: ").append(ascii(r.clientName)).append("\n");
            if (isNotEmpty(r.clientPhone)) {
                sb.append("Tel: ").append(ascii(r.clientPhone)).append("\n");
            }
            if (isNotEmpty(r.clientAddress)) {
                sb.append("Adr: ").append(ascii(r.clientAddress)).append("\n");
            }
        }

        sb.append(separator('=')).append("\n");
        sb.append(lineColumns("Article", "Qte", "P.U.", "Total")).append("\n");
        sb.append(separator('-')).append("\n");

        if (r.produits != null) {
            for (ReceiptLineDTO l : r.produits) {
                String designation = ascii(nvl(l.designation));
                String qte = String.valueOf(l.quantite);
                String pu = formatPrice(l.prixUnitaire);
                String total = formatPrice(l.montant);

                if (designation.length() > 20) {
                    sb.append(designation).append("\n");
                    sb.append(lineColumns("", qte, pu, total)).append("\n");
                } else {
                    sb.append(lineColumns(designation, qte, pu, total)).append("\n");
                }
            }
        }

        sb.append(separator('=')).append("\n");
        sb.append(lineLeftRight("TOTAL TTC:", formatPrice(r.totalTTC) + " Ar")).append("\n");
        sb.append(separator('-')).append("\n");

        if (isNotEmpty(r.modePaiement)) {
            sb.append(lineLeftRight("Mode:", ascii(r.modePaiement))).append("\n");
        }
        sb.append(lineLeftRight("Paye:", formatPrice(r.montantPaye) + " Ar")).append("\n");
        if (r.monnaie > 0) {
            sb.append(lineLeftRight("Monnaie:", formatPrice(r.monnaie) + " Ar")).append("\n");
        }

        sb.append(separator('=')).append("\n");
        sb.append(center("Merci pour votre achat !")).append("\n");
        sb.append(center("A bientot !")).append("\n");
        sb.append("\n\n\n");

        byte[] data = sb.toString().getBytes(StandardCharsets.US_ASCII);
        DocPrintJob job = service.createPrintJob();
        job.print(new SimpleDoc(data, DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
    }

    // ═══════════════════════════════════════════════════════════

    private String formatPrice(long amount) {
        String raw = Long.toString(Math.abs(amount));
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = raw.length() - 1; i >= 0; i--) {
            sb.append(raw.charAt(i));
            count++;
            if (count % 3 == 0 && i > 0) {
                sb.append(' ');
            }
        }
        String result = sb.reverse().toString();
        return amount < 0 ? "-" + result : result;
    }

    private String lineLeftRight(String left, String right) {
        int spaces = LINE_WIDTH - left.length() - right.length();
        if (spaces > 0) return left + " ".repeat(spaces) + right;
        return left + " " + right;
    }

    private String lineColumns(String article, String qte, String pu, String total) {
        return padEnd(article, 20) + padStart(qte, 5) + padStart(pu, 11) + padStart(total, 12);
    }

    private String center(String text) {
        if (text.length() >= LINE_WIDTH) return text.substring(0, LINE_WIDTH);
        int pad = (LINE_WIDTH - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private String padEnd(String s, int len) {
        if (s == null) s = "";
        if (s.length() > len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    private String padStart(String s, int len) {
        if (s == null) s = "";
        if (s.length() > len) return s.substring(0, len);
        return " ".repeat(len - s.length()) + s;
    }

    private String separator(char c) {
        return String.valueOf(c).repeat(LINE_WIDTH);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private boolean isNotEmpty(String s) { return s != null && !s.isEmpty(); }

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
                default: if (ch < 0x80) sb.append(ch); break;
            }
        }
        return sb.toString();
    }
}