package com.qbitspark.buildwisebackend.subcontractor_service.enums;

import lombok.Getter;

@Getter

public enum SpecializationType {
    ELECTRICAL("Electrical Work"),
    PLUMBING("Plumbing Work"),
    CARPENTRY("Carpentry Work"),
    ROOFING("Roofing Work"),
    PAINTING("Painting Work"),
    FLOORING("Flooring Work"),
    HVAC("HVAC Systems"),
    MASONRY("Masonry Work"),
    CONCRETE("Concrete Work"),
    STEEL_WORK("Steel Work"),
    LANDSCAPING("Landscaping"),
    DRYWALL("Drywall Installation"),
    INSULATION("Insulation Work"),
    WINDOWS_DOORS("Windows and Doors"),
    DEMOLITION("Demolition Work"),
    EXCAVATION("Excavation Work"),
    WATERPROOFING("Waterproofing"),
    TILE_WORK("Tile Work"),
    EXTERIOR_SIDING("Exterior Siding"),
    FOUNDATION("Foundation Work");

    private final String displayName;

    SpecializationType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}