package agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Une ligne produit d'un bon de livraison thermique (sans prix : c'est un
 * document de remise de marchandise, pas une facture).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryLineDTO {
    // Groupement par piece (optionnel) : titre / sous-titre.
    public String groupTitle;
    public String groupSubtitle;

    public String designation;   // nom complet du produit
    public String reference;     // reference produit (optionnel)
    public double quantite;      // supporte les decimaux (tissu au metre)
    public String unit;          // ex: "m" pour le tissu (optionnel)
    public String note;          // N.B ligne (optionnel)
}
