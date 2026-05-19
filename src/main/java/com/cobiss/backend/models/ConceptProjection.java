package com.cobiss.backend.models;

import java.util.List;

public interface ConceptProjection {
    String getUri();
    String getPrefLabelSl();
    String getPrefLabelEn();
    String getDefinition();
    List<String> getAltLabel();

    // These automatically recurse into projections or entities safely
    List<ConceptProjection> getBroader();
    List<ConceptProjection> getNarrower();
    List<ConceptProjection> getRelated();


    List<String> getRawPrefLabels();    // Re-apply your fallback logic safely onto the projection wrapper

    default String getPrefLabel() {
        return getPrefLabelSl() != null ? getPrefLabelSl() : getPrefLabelEn();
    }
}