package agent.dto;

public class ReceiptLineDTO {
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


