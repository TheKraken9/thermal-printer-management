package agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiptDTO {
    public String saleBoutiqueName;
    public String boutiqueName;
    public String boutiqueAddress;
    public String boutiquePhone;
    public String boutiqueNIF;
    public String boutiqueStat;

    public String numero;
    public String type;
    public String date;
    public String caissier;
    public String createdBy;   // Personne qui a cree la vente

    // Authentification du document
    public String verifyUrl;
    public String verifyCode;

    public String moreInfo;

    public String clientName;
    public String clientPhone;
    public String clientAddress;
    public String clientNIF;

    public List<ReceiptLineDTO> produits;

    public List<ReceiptExtraLineDTO> saleExtras;
    public Long totalExtras;


    // Livraison / retrait
    public String fulfillmentType;
    public String fulfillmentLabel;
    public String pickupBoutiqueName;
    public String deliveryAddress;
    public Long deliveryFee;
    public String fulfillmentDate;
    public String fulfillmentTime;
    public String fulfillmentTimeInstructions;

    // Totaux
    public Long sousTotal;
    public Long totalHT;
    public Long tva;
    public Double tauxTVA;

    // Remise globale
    public Boolean hasDiscount;
    public String discountType;
    public Double discountValue;
    public Long discountAmount;
    public String discountReason;
    public String discountLabel;

    public long totalTTC;
    public long montantPaye;
    public Long montantRestant;
    public long monnaie;

    public String modePaiement;
    public String messageRemerciement;
}