package agent.printer;

import agent.dto.ProformaDTO;
import agent.dto.ProformaLineDTO;

import javax.print.*;
import java.nio.charset.StandardCharsets;

/**
 * Impression thermique d'un proforma.
 * Même logique que WindowsPrinter mais adapté au format proforma :
 * - Pas de montant payé / monnaie rendue
 * - Mention "DEVIS - PROFORMA" bien visible
 * - Numéro de proforma en évidence
 * - Modes de paiement acceptés fixes (Espèces + Mobile Money)
 * - Mention de validité
 */
public class ProformaThermalPrinter {

    private static final int LINE_WIDTH = 48;

    public void print(ProformaDTO p) throws Exception {
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        if (service == null) throw new RuntimeException("Aucune imprimante Windows disponible");

        StringBuilder sb = new StringBuilder();

        sb.append(separator('=')).append("\n");
        sb.append(center("*** PROFORMA - DEVIS ***")).append("\n");
        sb.append(center("(CE N'EST PAS UN RECU DE VENTE)")).append("\n");
        sb.append(separator('=')).append("\n");

        if (isNotEmpty(p.saleBoutiqueName)) {
            sb.append(center("BOUTIQUE DE VENTE")).append("\n");
            sb.append(center(ascii(p.saleBoutiqueName))).append("\n");
            sb.append(separator('=')).append("\n");
        }

        appendCenteredByDash(sb, p.boutiqueName);

        if (isNotEmpty(p.boutiqueAddress)) {
            appendCenteredByDash(sb, p.boutiqueAddress);
        }

        if (isNotEmpty(p.boutiquePhone)) {
            sb.append(center("Tel: " + ascii(p.boutiquePhone))).append("\n");
        }

        if (isNotEmpty(p.boutiqueNIF)) {
            sb.append(center("NIF: " + ascii(p.boutiqueNIF))).append("\n");
        }

        if (isNotEmpty(p.boutiqueStat)) {
            sb.append(center("STAT: " + ascii(p.boutiqueStat))).append("\n");
        }

        sb.append(separator('=')).append("\n");
        sb.append(lineLeftRight("No: " + ascii(nvl(p.proformaNumber)),
                ascii(nvl(p.proformaDate)))).append("\n");

        // Validite : "30 jours a compter du JJ/MM/AAAA (jusqu'au JJ/MM/AAAA)"
        String vdays = isNotEmpty(p.validityDays) ? p.validityDays : "30 jours";
        if (isNotEmpty(p.validityFrom)) {
            sb.append("Validite: ").append(ascii(vdays))
              .append(" a compter du ").append(ascii(p.validityFrom)).append("\n");
            if (isNotEmpty(p.validityUntil)) {
                sb.append("          (jusqu'au ").append(ascii(p.validityUntil)).append(")\n");
            }
        } else {
            sb.append(lineLeftRight("Validite:", ascii(vdays))).append("\n");
        }

        if (isNotEmpty(p.createdBy)) {
            sb.append(lineLeftRight("Vendeuse:", ascii(p.createdBy))).append("\n");
        }

        if (isNotEmpty(p.clientName)) {
            sb.append(separator('-')).append("\n");
            sb.append("Client: ").append(ascii(p.clientName)).append("\n");

            if (isNotEmpty(p.clientPhone)) {
                sb.append("Tel:    ").append(ascii(p.clientPhone)).append("\n");
            }

            if (isNotEmpty(p.clientAddress)) {
                sb.append("Adresse: ")
                        .append(wrapText(ascii(p.clientAddress), LINE_WIDTH - 9))
                        .append("\n");
            }
        }

        boolean hasFulfillment = isNotEmpty(p.fulfillmentLabel)
                || isNotEmpty(p.pickupBoutiqueName)
                || isNotEmpty(p.deliveryAddress)
                || p.deliveryFee != null
                || isNotEmpty(p.fulfillmentDate);

        if (hasFulfillment) {
            sb.append(separator('-')).append("\n");

            if (isNotEmpty(p.fulfillmentLabel)) {
                sb.append("Mode: ").append(ascii(p.fulfillmentLabel)).append("\n");
            }

            if (isNotEmpty(p.pickupBoutiqueName)) {
                sb.append("Boutique: ").append(ascii(p.pickupBoutiqueName)).append("\n");
            }

            if (isNotEmpty(p.deliveryAddress)) {
                sb.append("Livraison: ")
                        .append(wrapText(ascii(p.deliveryAddress), LINE_WIDTH - 11))
                        .append("\n");
            }

            if (p.deliveryFee != null && p.deliveryFee > 0) {
                sb.append(lineLeftRight("Frais livraison:", formatPrice(p.deliveryFee) + " Ar")).append("\n");
            }

            if (isNotEmpty(p.fulfillmentDate)) {
                sb.append("Date prevue: ").append(ascii(p.fulfillmentDate)).append("\n");
                if (isNotEmpty(p.fulfillmentTime)) {
                    sb.append("Heure: ").append(ascii(p.fulfillmentTime)).append("\n");
                }
            }

            if (isNotEmpty(p.fulfillmentTimeInstructions)) {
                sb.append("Instruction: ")
                        .append(wrapText(ascii(p.fulfillmentTimeInstructions), LINE_WIDTH - 13))
                        .append("\n");
            }
        }

        sb.append(separator('=')).append("\n");
        sb.append(lineColumns("Article", "Qte", "P.U.", "Total")).append("\n");
        sb.append(separator('-')).append("\n");

        if (p.lignes != null) {
            String curGroupTitle = null;
            String curGroupSub = null;
            for (ProformaLineDTO l : p.lignes) {
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
                String qte   = formatQuantity(l.quantite);
                String pu    = formatPrice(l.prixUnitaire);
                String total = formatPrice(l.subtotal);

                if (designation.length() > 20) {
                    sb.append(wrapText(designation, LINE_WIDTH)).append("\n");
                    sb.append(lineColumns("", qte, pu, total)).append("\n");
                } else {
                    sb.append(lineColumns(designation, qte, pu, total)).append("\n");
                }

                // Badge personnalisé + dimensions
                if (Boolean.TRUE.equals(l.isCustomized)) {
                    if (isNotEmpty(l.curtainType)) {
                        sb.append("  Type: ").append(ascii(l.curtainType)).append("\n");
                    }
                    if (isNotEmpty(l.dimensions)) {
                        sb.append("  Dim:  ").append(ascii(l.dimensions)).append("\n");
                    }
                }

                if (isNotEmpty(l.otherInfo)) {
                    sb.append("  Note: ")
                            .append(wrapText(ascii(l.otherInfo), LINE_WIDTH - 8))
                            .append("\n");
                }
            }
        }

        sb.append(separator('=')).append("\n");

        // Totaux masques si showTotal == false (devis sans montants).
        if (!Boolean.FALSE.equals(p.showTotal)) {
            if (p.sousTotal != null && p.sousTotal > 0) {
                sb.append(lineLeftRight("Sous-total:", formatPrice(p.sousTotal) + " Ar")).append("\n");
            }

            if (p.deliveryFeeTotal != null && p.deliveryFeeTotal > 0) {
                sb.append(lineLeftRight("Livraison:", formatPrice(p.deliveryFeeTotal) + " Ar")).append("\n");
            }

            if (Boolean.TRUE.equals(p.hasDiscount)
                    && p.discountAmount != null && p.discountAmount > 0) {

                String discLabel = buildDiscountLabel(p.discountType, p.discountValue, p.discountAmount);
                sb.append(lineLeftRight("Remise:", discLabel)).append("\n");

                if (isNotEmpty(p.discountReason)) {
                    sb.append("Raison: ")
                            .append(wrapText(ascii(p.discountReason), LINE_WIDTH - 8))
                            .append("\n");
                }
            }

            sb.append(separator('-')).append("\n");
            sb.append(lineLeftRight("TOTAL ESTIME TTC:", formatPrice(p.totalEstime) + " Ar")).append("\n");
            sb.append(separator('-')).append("\n");
        }

        sb.append("\n");
        sb.append(center("Modes de paiement acceptes")).append("\n");
        sb.append(center("Especes | Mobile Money | Cheque")).append("\n");

        if (isNotEmpty(p.otherInfo)) {
            sb.append(separator('-')).append("\n");
            appendMultiline(sb, "Notes: ", ascii(p.otherInfo), LINE_WIDTH - 7);
        }

        sb.append(separator('=')).append("\n");
        sb.append(center("Ce document est un devis estimatif")).append("\n");
        sb.append(center("et ne constitue pas une facture.")).append("\n");
        sb.append(separator('-')).append("\n");
        sb.append(center("Merci pour votre confiance !")).append("\n");
        sb.append(center("Ref: " + ascii(nvl(p.proformaNumber)))).append("\n");

        if (isNotEmpty(p.verifyCode)) {
            sb.append(separator('-')).append("\n");
            sb.append(center("Code d'authenticite")).append("\n");
            sb.append(center(ascii(p.verifyCode))).append("\n");
        }

        sb.append("\n\n\n\n");

        String ticketText = sb.toString();

        // ── DEBUG : apercu du proforma dans la console de l'agent ────────────
        System.out.println("\n========== PROFORMA " + nvl(p.proformaNumber) + " ==========");
        System.out.println("Imprimante : " + service.getName());
        System.out.println("Nb lignes : " + (p.lignes != null ? p.lignes.size() : 0)
                + " | showTotal : " + p.showTotal);
        System.out.println("----------------------------------------------------");
        System.out.println(ticketText);
        System.out.println("========== FIN PROFORMA ==========\n");

        byte[] textData = ticketText.getBytes(StandardCharsets.US_ASCII);
        byte[] cut      = new byte[]{ 0x1D, 0x56, 0x00 };
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

    private String lineColumns(String article, String qte, String pu, String total) {
        return padEnd(article, 20)
                + padStart(qte, 5)
                + padStart(pu, 11)
                + padStart(total, 12);
    }

    private String center(String text) {
        text = nvl(text);
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

    private void appendCenteredByDash(StringBuilder sb, String text) {
        if (!isNotEmpty(text)) return;
        String cleanText = ascii(text);
        String[] parts = cleanText.split("\\s*[-\u2013\u2014]\\s*");
        for (String part : parts) {
            if (isNotEmpty(part)) sb.append(center(part.trim())).append("\n");
        }
    }

    private String buildDiscountLabel(String type, Double value, Long amount) {
        if (amount == null || amount <= 0) return "";
        String formattedAmount = "-" + formatPrice(amount) + " Ar";
        if ("percentage".equalsIgnoreCase(type) && value != null && value > 0) {
            return formatPercent(value) + "% (" + formattedAmount + ")";
        }
        return formattedAmount;
    }

    private String formatPercent(Double value) {
        if (value == null) return "";
        if (value == Math.floor(value)) return String.valueOf(value.longValue());
        return String.valueOf(value);
    }
}