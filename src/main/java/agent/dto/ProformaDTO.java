package agent.dto;

import java.util.List;

/**
 * DTO envoyé par Angular au Print Agent pour l'impression thermique d'un proforma.
 * Distinct de ReceiptDTO car un proforma n'a pas de montant payé,
 * ni de caissier, ni de monnaie rendue.
 */
public class ProformaDTO {

    // ── Entreprise ────────────────────────────────────────────────────────
    public String boutiqueName;
    public String boutiqueAddress;
    public String boutiquePhone;
    public String boutiqueNIF;
    public String boutiqueStat;

    // ── Lieu de la vente + auteur ─────────────────────────────────────────
    public String saleBoutiqueName;  // Boutique / lieu ou le proforma a ete etabli
    public String createdBy;         // Personne qui a cree le proforma

    // ── Entête document ───────────────────────────────────────────────────
    public String proformaNumber;   // ex: PRO-2026-042
    public String proformaDate;     // ex: 03/06/2026
    public String validityDays;     // ex: "30 jours"
    public String validityFrom;     // date de debut de validite, ex: 16/07/2026
    public String validityUntil;    // date d'expiration, ex: 15/08/2026

    // ── Client ────────────────────────────────────────────────────────────
    public String clientName;
    public String clientPhone;
    public String clientAddress;

    // ── Livraison / retrait ───────────────────────────────────────────────
    public String fulfillmentType;          // "delivery" | "pickup" | null
    public String fulfillmentLabel;         // "Livraison a domicile" / "Retrait en boutique"
    public String pickupBoutiqueName;
    public String deliveryAddress;
    public Long   deliveryFee;
    public String fulfillmentDate;
    public String fulfillmentTime;
    public String fulfillmentTimeInstructions;

    // ── Produits ──────────────────────────────────────────────────────────
    public List<ProformaLineDTO> lignes;

    // ── Totaux ────────────────────────────────────────────────────────────
    public Long   sousTotal;
    public Long   deliveryFeeTotal;
    public Boolean hasDiscount;
    public String discountType;
    public Double discountValue;
    public Long   discountAmount;
    public String discountReason;
    public long   totalEstime;      // TOTAL ESTIMÉ TTC

    // ── Notes ─────────────────────────────────────────────────────────────
    public String otherInfo;

    // ── Authentification du document ──────────────────────────────────────
    public String verifyUrl;
    public String verifyCode;
}