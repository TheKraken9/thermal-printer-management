package agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Une ligne produit dans le proforma thermique.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProformaLineDTO {
    // Groupement par piece : titre (ex: "Salon") et sous-titre (ex: "Fenetre 1").
    public String  groupTitle;
    public String  groupSubtitle;

    public String  designation;     // nom complet du produit
    public double  quantite;        // supporte les décimaux (tissu au mètre)
    public long    prixUnitaire;
    public long    subtotal;
    public Boolean isCustomized;
    public String  curtainType;     // "Rideau Lourd" / "Voile" / etc.
    public String  dimensions;      // ex: "2.5m x 2.3m"
    public String  otherInfo;       // note optionnelle
}