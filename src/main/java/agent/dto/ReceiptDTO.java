package agent.dto;

import java.util.List;

public class ReceiptDTO {
    public String boutiqueName;
    public String boutiqueAddress;
    public String boutiquePhone;
    public String boutiqueNIF;
    public String boutiqueStat;

    public String numero;
    public String type;
    public String date;
    public String caissier;

    public String clientName;
    public String clientPhone;
    public String clientAddress;
    public String clientNIF;

    public List<ReceiptLineDTO> produits;

    public long totalTTC;
    public long montantPaye;
    public long monnaie;

    public String modePaiement;
}