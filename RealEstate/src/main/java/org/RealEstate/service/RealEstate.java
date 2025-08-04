package org.RealEstate.service;

public final class RealEstate {
    private static volatile RealEstate instance;

    private RealEstate() {}

    public static RealEstate getInstance() {
        if (instance == null) {
            synchronized (RealEstate.class) {
                if (instance == null) {
                    instance = new RealEstate();
                }
            }
        }
        return instance;
    }

}
