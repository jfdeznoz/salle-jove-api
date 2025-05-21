package com.sallejoven.backend.model.enums;

public enum TshirtSizeEnum {
    XS, S, M, L, XL, XXL, XXXL;

    public static TshirtSizeEnum fromIndex(Integer idx) {
        if (idx == null) return null;
        switch (idx) {
            case 0:  return XS;
            case 1:  return S;
            case 2:  return M;
            case 3:  return L;
            case 4:  return XL;
            case 5:  return XXL;
            case 6:  return XXXL;
            default: return null;
        }
    }
}