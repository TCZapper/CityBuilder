package com.jasperb.citybuilder.util;

import com.jasperb.citybuilder.CityModel;

public class ObjectEdit {
    int id;
    int type;
    int row;
    int col;
    
    public ObjectEdit(int row, int col, int type, int id) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.id = id;
    }
    
    public void addObject(CityModel model) {
        model.addObject(row, col, type, id);
    }
}
