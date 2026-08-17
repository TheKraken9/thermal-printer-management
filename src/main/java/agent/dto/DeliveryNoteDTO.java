package agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO envoyé par Angular au Print Agent pour l'impression thermique d'un
 * BON DE LIVRAISON. Document de remise de marchandise : pas de prix ni de
 * paiement, focalisé sur le destinataire et les quantités livrées.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryNoteDTO {

    // ── Entreprise ────────────────────────────────────────────────────────
    public String boutiqueName;
    public String boutiqueAddress;
    public String boutiquePhone;
    public String boutiqueNIF;
    public String boutiqueStat;

    // ── Lieu de la vente + auteur ─────────────────────────────────────────
    public String saleBoutiqueName;  // magasin d'origine
    public String deliveredByName;   // livreur / personne qui remet

    // ── Entête document ───────────────────────────────────────────────────
    public String numero;            // ex: ANDR-2026-289
    public String date;              // ex: 17/08/2026
    public String time;              // ex: 13:07 (heure prevue, optionnel)

    // ── Destinataire ──────────────────────────────────────────────────────
    public String clientName;
    public String clientPhone;
    public String deliveryAddress;
    public String instructions;

    public String note;              // note (otherInfo)
    public String moreInfo;          // infos supplementaires (additionalInfo)

    // ── Produits livres ───────────────────────────────────────────────────
    public List<DeliveryLineDTO> lignes;

    // ── Frais de livraison (inclus dans le total) ─────────────────────────
    public Long deliveryFee;

    // ── Montants pour le livreur (encaissement à la livraison) ────────────
    public Long totalAmount;      // total TTC de la vente (frais inclus)
    public Long amountPaid;       // déjà payé
    public Long remainingAmount;  // reste à encaisser à la livraison

    // ── Authentification du document ──────────────────────────────────────
    public String verifyUrl;
    public String verifyCode;
}
