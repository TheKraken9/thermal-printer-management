package agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiptLineDTO {

    // Groupement par piece : titre (ex: "Salon") et sous-titre (ex: "Fenetre 1").
    public String groupTitle;
    public String groupSubtitle;

    public String designation;
    public int quantite;
    public long prixUnitaire;

    // Montant avant remise ligne
    public Long montantBrut;

    // Remise par produit
    public String remiseType;
    public Double remiseValeur;
    public Long remiseMontant;
    public String remiseLabel;

    // Montant final de la ligne
    public long montant;

    // Infos supplementaires
    public String dimensions;
    public Boolean isCustomized;
    public String note;
    public String unit;
}


