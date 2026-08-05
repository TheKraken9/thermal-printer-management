package agent.printer;

import agent.dto.ReceiptDTO;
import agent.dto.ReceiptLineDTO;
import com.github.anastaciocintra.escpos.*;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class LinuxPrinter {

    private static final String DEVICE = "/dev/usb/lp0";
    private static final int LINE_WIDTH = 48;

    public void print(ReceiptDTO r) throws Exception {

        OutputStream os = new FileOutputStream(DEVICE);
        EscPos escpos = new EscPos(os);

        Style titleStyle = new Style()
                .setBold(true)
                .setFontSize(Style.FontSize._1, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center);

        Style subtitleStyle = new Style()
                .setBold(true)
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Center);

        Style normalStyle = new Style()
                .setBold(false)
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);

        Style boldStyle = new Style()
                .setBold(true)
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);

        Style centerStyle = new Style()
                .setBold(false)
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Center);

        escpos.writeLF(titleStyle, ascii(r.boutiqueName));

        if (isNotEmpty(r.boutiqueAddress)) {
            String[] parts = r.boutiqueAddress.split("\\s*-\\s*");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    escpos.writeLF(centerStyle, ascii(trimmed));
                }
            }
        }
        if (isNotEmpty(r.boutiquePhone)) {
            escpos.writeLF(centerStyle, "Tel: " + ascii(r.boutiquePhone));
        }
        if (isNotEmpty(r.boutiqueNIF)) {
            escpos.writeLF(centerStyle, "NIF: " + ascii(r.boutiqueNIF));
        }
        if (isNotEmpty(r.boutiqueStat)) {
            escpos.writeLF(centerStyle, "STAT: " + ascii(r.boutiqueStat));
        }

        escpos.writeLF(subtitleStyle, r.type != null && r.type.equals("FACTURE") ? "FACTURE" : "RECU DE VENTE");

        escpos.writeLF(normalStyle, separator('='));

        escpos.writeLF(normalStyle, lineLeftRight("No: " + ascii(nvl(r.numero)), ascii(nvl(r.date))));

        if (isNotEmpty(r.caissier)) {
            escpos.writeLF(normalStyle, "Caissiere: " + ascii(r.caissier));
        }

        if (isNotEmpty(r.clientName)) {
            escpos.writeLF(normalStyle, separator('='));
            escpos.writeLF(normalStyle, "Client: " + ascii(r.clientName));
            if (isNotEmpty(r.clientPhone)) {
                escpos.writeLF(normalStyle, "Tel: " + ascii(r.clientPhone));
            }
            if (isNotEmpty(r.clientAddress)) {
                escpos.writeLF(normalStyle, "Adr: " + ascii(r.clientAddress));
            }
        }

        escpos.writeLF(normalStyle, separator('='));

        escpos.writeLF(boldStyle, lineColumns("Article", "Qte", "P.U.", "Total"));
        escpos.writeLF(normalStyle, separator('-'));

        if (r.produits != null) {
            for (ReceiptLineDTO l : r.produits) {
                String designation = ascii(nvl(l.designation));
                String qte = String.valueOf(l.quantite);
                String pu = formatPrice(l.prixUnitaire);
                String total = formatPrice(l.montant);

                if (designation.length() > 20) {
                    escpos.writeLF(normalStyle, designation);
                    escpos.writeLF(normalStyle, lineColumns("", qte, pu, total));
                } else {
                    escpos.writeLF(normalStyle, lineColumns(designation, qte, pu, total));
                }
            }
        }

        escpos.writeLF(normalStyle, separator('='));

        escpos.writeLF(boldStyle, lineLeftRight("TOTAL TTC:", formatPrice(r.totalTTC) + " Ar"));
        escpos.writeLF(normalStyle, separator('-'));

        if (isNotEmpty(r.modePaiement)) {
            escpos.writeLF(normalStyle, lineLeftRight("Mode:", ascii(r.modePaiement)));
        }

        escpos.writeLF(normalStyle, lineLeftRight("Paye:", formatPrice(r.montantPaye) + " Ar"));

        long reste = r.totalTTC - r.montantPaye;
        if (reste > 0) {
            escpos.writeLF(boldStyle, lineLeftRight("RESTE:", formatPrice(reste) + " Ar"));
        }

        if (r.monnaie > 0) {
            escpos.writeLF(normalStyle, lineLeftRight("Monnaie:", formatPrice(r.monnaie) + " Ar"));
        }

        escpos.writeLF(normalStyle, separator('='));
        escpos.writeLF(centerStyle, "Merci pour votre achat !");
        escpos.writeLF(centerStyle, "A bientot !");

        escpos.feed(4);
        escpos.cut(EscPos.CutMode.FULL);

        escpos.close();
        os.close();
    }


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
        if (spaces > 0) {
            return left + " ".repeat(spaces) + right;
        }
        return left + " " + right;
    }

    private String lineColumns(String article, String qte, String pu, String total) {
        return padEnd(article, 20) + padStart(qte, 5) + padStart(pu, 11) + padStart(total, 12);
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

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
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