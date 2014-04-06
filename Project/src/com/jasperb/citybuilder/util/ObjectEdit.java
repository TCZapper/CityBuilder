package com.jasperb.citybuilder.util;

import com.jasperb.citybuilder.CityModel;

public class ObjectEdit {
    public enum EDIT_TYPE {
        ADD, REMOVE
    };

    int id;
    int type;
    int row;
    int col;
    EDIT_TYPE editType;

    public ObjectEdit(EDIT_TYPE editType, int row, int col, int type, int id) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.id = id;
        this.editType = editType;
    }

    public ObjectEdit(EDIT_TYPE editType, int id) {
        this.id = id;
        this.editType = editType;
    }

    public void processEdit(CityModel model) {
        switch (editType) {
        case ADD:
            model.addObject(row, col, type, id);
            break;
        case REMOVE:
            model.removeObject(id);
            break;
        }
    }

    public boolean equals(ObjectEdit o) {
        if (editType != o.editType)
            return false;

        switch (editType) {
        case ADD:
            return o.id == id && o.row == row && o.col == col && o.type == type;
        default:
            return o.id == id;
        }
    }
}
