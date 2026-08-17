package agent.printer;

import agent.dto.ReceiptDTO;
import agent.dto.ReceiptExtraLineDTO;
import agent.dto.ReceiptLineDTO;

import javax.print.*;
import java.nio.charset.StandardCharsets;

public class WindowsPrinter {

    private static final int LINE_WIDTH = 48;

    public void print(ReceiptDTO r) throws Exception {
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        if (service == null) throw new RuntimeException("Aucune imprimante Windows");

        StringBuilder sb = new StringBuilder();

        String docTypeLabel;
        if ("FACTURE".equals(r.type)) {
            docTypeLabel = "FACTURE";
        } else if ("ECHANGE".equals(r.type)) {
            docTypeLabel = "BON D'ECHANGE";
        } else {
            docTypeLabel = "RECU DE VENTE";
        }
        sb.append(separator('=')).append("\n");
        sb.append(center("*** " + docTypeLabel + " ***")).append("\n");
        sb.append(separator('=')).append("\n");

        sb.append(center("BOUTIQUE DE VENTE")).append("\n");
        sb.append(center(ascii(resolveSaleBoutiqueName(r)))).append("\n");
        sb.append(separator('=')).append("\n");

        appendCenteredByDash(sb, r.boutiqueName);

        if (isNotEmpty(r.boutiqueAddress)) {
            appendCenteredByDash(sb, r.boutiqueAddress);
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

        sb.append(separator('=')).append("\n");

        sb.append(lineLeftRight("No: " + ascii(nvl(r.numero)), ascii(nvl(r.date)))).append("\n");

        if (isNotEmpty(r.caissier)) {
            sb.append("Caissiere: ").append(ascii(r.caissier)).append("\n");
        }

        if (isNotEmpty(r.createdBy)) {
            sb.append("Vendeuse: ").append(ascii(r.createdBy)).append("\n");
        }

        if (isNotEmpty(r.moreInfo)) {
            appendMultiline(sb, "Info: ", ascii(r.moreInfo), LINE_WIDTH - 6);
        }

        // Client
        if (isNotEmpty(r.clientName)) {
            sb.append(separator('=')).append("\n");
            sb.append("Client: ").append(ascii(r.clientName)).append("\n");

            if (isNotEmpty(r.clientPhone)) {
                sb.append("Tel: ").append(ascii(r.clientPhone)).append("\n");
            }

            if (isNotEmpty(r.clientNIF)) {
                sb.append("NIF client: ").append(ascii(r.clientNIF)).append("\n");
            }
        }

        // Livraison / retrait
        if (isNotEmpty(r.fulfillmentLabel)
                || isNotEmpty(r.pickupBoutiqueName)
                || isNotEmpty(r.deliveryAddress)
                || r.deliveryFee != null
                || isNotEmpty(r.fulfillmentDate)
                || isNotEmpty(r.fulfillmentTime)
                || isNotEmpty(r.fulfillmentTimeInstructions)) {

            sb.append(separator('=')).append("\n");

            if (isNotEmpty(r.fulfillmentLabel)) {
                sb.append("Mode: ").append(ascii(r.fulfillmentLabel)).append("\n");
            }

            if (isNotEmpty(r.pickupBoutiqueName)) {
                sb.append("Boutique: ").append(ascii(r.pickupBoutiqueName)).append("\n");
            }

            if (isNotEmpty(r.deliveryAddress)) {
                sb.append("Livraison: ")
                        .append(wrapText(ascii(r.deliveryAddress), LINE_WIDTH - 11))
                        .append("\n");
            }

            if (r.deliveryFee != null && r.deliveryFee > 0) {
                sb.append(lineLeftRight("Frais livraison:", formatPrice(r.deliveryFee) + " Ar")).append("\n");
            }

            if (isNotEmpty(r.fulfillmentDate)) {
                sb.append("Date prevue: ").append(ascii(r.fulfillmentDate)).append("\n");
            }

            if (isNotEmpty(r.fulfillmentTime)) {
                sb.append("Heure: ").append(ascii(r.fulfillmentTime)).append("\n");
            }

            if (isNotEmpty(r.fulfillmentTimeInstructions)) {
                sb.append("Instruction: ")
                        .append(wrapText(ascii(r.fulfillmentTimeInstructions), LINE_WIDTH - 13))
                        .append("\n");
            }
        }

        // Produits
        sb.append(separator('=')).append("\n");
        sb.append(lineColumns("Article", "Qte", "P.U.", "Total")).append("\n");
        sb.append(separator('-')).append("\n");

        if (r.produits != null) {
            String curGroupTitle = null;
            String curGroupSub = null;
            for (ReceiptLineDTO l : r.produits) {
                // En-tete de groupe (piece / sous-titre) imprime au changement.
                if (isNotEmpty(l.groupTitle) && !l.groupTitle.equals(curGroupTitle)) {
                    curGroupTitle = l.groupTitle;
                    curGroupSub = null;
                    sb.append(center(">> " + ascii(l.groupTitle).toUpperCase() + " <<")).append("\n");
                }
                if (isNotEmpty(l.groupSubtitle) && !l.groupSubtitle.equals(curGroupSub)) {
                    curGroupSub = l.groupSubtitle;
                    sb.append("* ").append(ascii(l.groupSubtitle)).append("\n");
                }

                String designation = ascii(nvl(l.designation));
                String qte = String.valueOf(l.quantite);
                String pu = formatPrice(l.prixUnitaire);
                String total = formatPrice(l.montant);

                if (designation.length() > 20) {
                    sb.append(wrapText(designation, LINE_WIDTH)).append("\n");
                    sb.append(lineColumns("", qte, pu, total)).append("\n");
                } else {
                    sb.append(lineColumns(designation, qte, pu, total)).append("\n");
                }

                if (l.montantBrut != null && l.montantBrut > l.montant) {
                    sb.append(lineLeftRight("  Brut:", formatPrice(l.montantBrut) + " Ar")).append("\n");
                }

                if (l.remiseMontant != null && l.remiseMontant > 0) {
                    String label = buildDiscountLabel(
                            l.remiseType,
                            l.remiseValeur,
                            l.remiseMontant
                    );

                    sb.append(lineLeftRight("  Remise:", label)).append("\n");
                }

                if (isNotEmpty(l.note)) {
                    sb.append("  Note: ")
                            .append(wrapText(ascii(l.note), LINE_WIDTH - 8))
                            .append("\n");
                }
            }
        }


        // Frais supplémentaires
        if (r.saleExtras != null && !r.saleExtras.isEmpty()) {
            sb.append(separator('=')).append("\n");
            sb.append(center("FRAIS SUPPLEMENTAIRES")).append("\n");
            sb.append(separator('-')).append("\n");

            for (ReceiptExtraLineDTO extra : r.saleExtras) {
                // Libellé + date
                String labelLine = ascii(nvl(extra.label));
                if (isNotEmpty(extra.executionDate)) {
                    labelLine += " (" + extra.executionDate + ")";
                }

                // Ligne principale : libellé | qte x PU | total
                String qteStr   = formatQuantity(extra.quantity) + " x " + formatPrice(extra.unitPrice) + " Ar";
                String totalStr = formatPrice(extra.totalAmount) + " Ar";

                if (labelLine.length() > 20) {
                    sb.append(wrapText(labelLine, LINE_WIDTH)).append("\n");
                    sb.append(lineLeftRight("  " + qteStr, totalStr)).append("\n");
                } else {
                    sb.append(lineColumns(labelLine, "", qteStr, totalStr)).append("\n");
                }

                // Statut paiement
                String statusLabel = switch (nvl(extra.paymentStatus)) {
                    case "completed" -> "Regle";
                    case "partial"   -> "Partiel (" + formatPrice(extra.remainingAmount) + " Ar restant)";
                    default          -> "En attente";
                };
                sb.append(lineLeftRight("  Statut:", statusLabel)).append("\n");

                // Note
                if (isNotEmpty(extra.note)) {
                    sb.append("  Note: ")
                            .append(wrapText(ascii(extra.note), LINE_WIDTH - 8))
                            .append("\n");
                }
            }

            // Sous-total frais supplémentaires
            if (r.totalExtras != null && r.totalExtras > 0) {
                sb.append(separator('-')).append("\n");
                sb.append(lineLeftRight("Total frais supp.:", formatPrice(r.totalExtras) + " Ar")).append("\n");
            }
        }


        // Totaux
        sb.append(separator('=')).append("\n");

        if (r.sousTotal != null && r.sousTotal > 0) {
            sb.append(lineLeftRight("Sous-total:", formatPrice(r.sousTotal) + " Ar")).append("\n");
        }

        if (r.totalHT != null && r.totalHT > 0) {
            sb.append(lineLeftRight("Total HT:", formatPrice(r.totalHT) + " Ar")).append("\n");
        }

        if (r.tva != null && r.tva > 0) {
            String labelTva = "TVA:";
            if (r.tauxTVA != null && r.tauxTVA > 0) {
                labelTva = "TVA " + r.tauxTVA + "%:";
            }
            sb.append(lineLeftRight(labelTva, formatPrice(r.tva) + " Ar")).append("\n");
        }

        if (r.deliveryFee != null && r.deliveryFee > 0) {
            sb.append(lineLeftRight("Livraison:", formatPrice(r.deliveryFee) + " Ar")).append("\n");
        }

        if (r.discountAmount != null && r.discountAmount > 0) {
            String label = buildDiscountLabel(
                    r.discountType,
                    r.discountValue,
                    r.discountAmount
            );

            sb.append(lineLeftRight("Remise:", label)).append("\n");

            if (isNotEmpty(r.discountReason)) {
                sb.append("Raison: ")
                        .append(wrapText(ascii(r.discountReason), LINE_WIDTH - 8))
                        .append("\n");
            }
        }

        sb.append(separator('-')).append("\n");
        sb.append(lineLeftRight("TOTAL TTC:", formatPrice(r.totalTTC) + " Ar")).append("\n");
        sb.append(separator('-')).append("\n");

        if (isNotEmpty(r.modePaiement)) {
            String[] modes = ascii(r.modePaiement).split("\\n");

            if (modes.length > 0) {
                sb.append("Mode: ").append(modes[0]).append("\n");

                for (int i = 1; i < modes.length; i++) {
                    sb.append("      ").append(modes[i]).append("\n");
                }
            }
        }

        if (r.montantPaye > 0) {
            sb.append(lineLeftRight("Paye:", formatPrice(r.montantPaye) + " Ar")).append("\n");
        }

        long reste = r.montantRestant != null
                ? r.montantRestant
                : r.totalTTC - r.montantPaye;

        if (reste > 0) {
            sb.append(lineLeftRight("RESTE:", formatPrice(reste) + " Ar")).append("\n");
        }

        if (r.monnaie > 0) {
            sb.append(lineLeftRight("Monnaie:", formatPrice(r.monnaie) + " Ar")).append("\n");
        }

        sb.append(separator('=')).append("\n");

        if (isNotEmpty(r.messageRemerciement)) {
            sb.append(center(ascii(r.messageRemerciement))).append("\n");
        } else {
            sb.append(center("Merci pour votre achat !")).append("\n");
        }

        sb.append(center("A bientot !")).append("\n");

        if (isNotEmpty(r.verifyCode)) {
            sb.append(separator('-')).append("\n");
            sb.append(center("Code d'authenticite")).append("\n");
            sb.append(center(ascii(r.verifyCode))).append("\n");
        }

        sb.append("\n\n\n");

        sb.append("\n\n\n\n");

        String ticketText = sb.toString();

        // ── DEBUG : apercu du ticket dans la console de l'agent ──────────────
        System.out.println("\n========== TICKET (" + docTypeLabel + ") ==========");
        System.out.println("Imprimante : " + service.getName());
        System.out.println("Nb produits : " + (r.produits != null ? r.produits.size() : 0)
                + " | Frais supp. : " + (r.saleExtras != null ? r.saleExtras.size() : 0));
        System.out.println("----------------------------------------------------");
        System.out.println(ticketText);
        System.out.println("========== FIN TICKET ==========\n");

        //byte[] textData = sb.toString().getBytes(StandardCharsets.US_ASCII);
        byte[] textData = ticketText.getBytes(StandardCharsets.US_ASCII);

// commande ESC/POS pour couper
        byte[] cut = new byte[]{0x1D, 0x56, 0x00};

// concaténer
        byte[] finalData = new byte[textData.length + cut.length];
        System.arraycopy(textData, 0, finalData, 0, textData.length);
        System.arraycopy(cut, 0, finalData, textData.length, cut.length);

        DocPrintJob job = service.createPrintJob();
        job.print(new SimpleDoc(finalData, DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
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
        left = nvl(left);
        right = nvl(right);

        int spaces = LINE_WIDTH - left.length() - right.length();

        if (spaces > 0) {
            return left + " ".repeat(spaces) + right;
        }

        return left + " " + right;
    }

    private String lineColumns(String article, String qte, String pu, String total) {
        return padEnd(article, 20)
                + padStart(qte, 5)
                + padStart(pu, 11)
                + padStart(total, 12);
    }

    private String center(String text) {
        text = nvl(text);

        if (text.length() >= LINE_WIDTH) {
            return text.substring(0, LINE_WIDTH);
        }

        int pad = (LINE_WIDTH - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private String padEnd(String s, int len) {
        if (s == null) s = "";

        if (s.length() > len) {
            return s.substring(0, len);
        }

        return s + " ".repeat(len - s.length());
    }

    private String padStart(String s, int len) {
        if (s == null) s = "";

        if (s.length() > len) {
            return s.substring(0, len);
        }

        return " ".repeat(len - s.length()) + s;
    }

    private String separator(char c) {
        return String.valueOf(c).repeat(LINE_WIDTH);
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

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

    /**
     * Ajoute un texte multi-ligne en respectant les retours a la ligne (\n)
     * saisis par l'utilisateur ; chaque ligne est en plus repliee a la largeur.
     * La 1re ligne est prefixee par `prefix`, les suivantes sont indentees.
     */
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

    private String ascii(String input) {
        if (input == null) return "";

        StringBuilder sb = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            switch (ch) {
                case '\u00e0':
                case '\u00e2':
                case '\u00e4':
                case '\u00e1':
                    sb.append('a');
                    break;

                case '\u00e8':
                case '\u00ea':
                case '\u00eb':
                case '\u00e9':
                    sb.append('e');
                    break;

                case '\u00ec':
                case '\u00ee':
                case '\u00ef':
                case '\u00ed':
                    sb.append('i');
                    break;

                case '\u00f2':
                case '\u00f4':
                case '\u00f6':
                case '\u00f3':
                    sb.append('o');
                    break;

                case '\u00f9':
                case '\u00fb':
                case '\u00fc':
                case '\u00fa':
                    sb.append('u');
                    break;

                case '\u00e7':
                    sb.append('c');
                    break;

                case '\u00f1':
                    sb.append('n');
                    break;

                case '\u00c0':
                case '\u00c2':
                case '\u00c4':
                case '\u00c1':
                    sb.append('A');
                    break;

                case '\u00c8':
                case '\u00ca':
                case '\u00cb':
                case '\u00c9':
                    sb.append('E');
                    break;

                case '\u00cc':
                case '\u00ce':
                case '\u00cf':
                case '\u00cd':
                    sb.append('I');
                    break;

                case '\u00d2':
                case '\u00d4':
                case '\u00d6':
                case '\u00d3':
                    sb.append('O');
                    break;

                case '\u00d9':
                case '\u00db':
                case '\u00dc':
                case '\u00da':
                    sb.append('U');
                    break;

                case '\u00c7':
                    sb.append('C');
                    break;

                case '\u00d1':
                    sb.append('N');
                    break;

                case '\u00b0':
                    sb.append('o');
                    break;

                case '\u00ab':
                case '\u00bb':
                    sb.append('"');
                    break;

                case '\u2018':
                case '\u2019':
                    sb.append('\'');
                    break;

                case '\u201c':
                case '\u201d':
                    sb.append('"');
                    break;

                case '\u2013':
                case '\u2014':
                    sb.append('-');
                    break;

                case '\u00a0':
                case '\u202f':
                    sb.append(' ');
                    break;

                default:
                    if (ch < 0x80) {
                        sb.append(ch);
                    }
                    break;
            }
        }

        return sb.toString();
    }

    private void appendCenteredByDash(StringBuilder sb, String text) {
        if (!isNotEmpty(text)) return;

        String cleanText = ascii(text);
        String[] parts = cleanText.split("\\s*[-–—]\\s*");

        for (String part : parts) {
            if (isNotEmpty(part)) {
                sb.append(center(part.trim())).append("\n");
            }
        }
    }

    private String buildDiscountLabel(String type, Double value, Long amount) {
        if (amount == null || amount <= 0) {
            return "";
        }
        String formattedAmount = "-" + formatPrice(amount) + " Ar";

        if ("percentage".equalsIgnoreCase(type) && value != null && value > 0) {
            return formatPercent(value) + "% (" + formattedAmount + ")";
        }

        return formattedAmount;
    }

    private String formatPercent(Double value) {
        if (value == null) return "";

        if (value == Math.floor(value)) {
            return String.valueOf(value.longValue());
        }

        return String.valueOf(value);
    }

    private String formatQuantity(double qty) {
        if (qty == Math.floor(qty)) {
            return String.valueOf((long) qty);
        }
        return String.valueOf(qty);
    }

    private String resolveSaleBoutiqueName(ReceiptDTO r) {
        if (r == null) return "Non specifie";

        if (isNotEmpty(r.saleBoutiqueName)) {
            return r.saleBoutiqueName;
        }

        String numero = nvl(r.numero);

        if (numero.startsWith("ANTA-")) {
            return "Antanimena";
        }

        if (numero.startsWith("ANDR-")) {
            return "Andrefan'Ambohijanahary";
        }

        if (numero.startsWith("AMP-")) {
            return "Ampefiloha";
        }

        return "Non specifie";
    }

}