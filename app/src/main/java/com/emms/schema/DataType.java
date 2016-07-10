package com.emms.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.jaffer_datastore_android_sdk.schema.Identity;
import com.jaffer_datastore_android_sdk.schema.Model;

/**
 * Created by jaffer.deng on 2016/7/10.
 */
@DatabaseTable(tableName = "DataType")
public class DataType extends Model<Operator, Long> implements Identity<Long> {

    public static final String DATATYPE_ID = "DataType_ID";
    public static final String DATATYPE_CODE = "DataTypeCode";
    public static final String DATATYPE_NAME = "DataTypeName";
    public static final String DATATYPE_DESCR = "DataTypeDescr";
    public static final String MODEL = "Model";
    public static final String STATUS = "Status";

    @DatabaseField(id = true,
            columnName = DATATYPE_ID, canBeNull = false)
    @SerializedName(DATATYPE_ID)
    @Expose
    private Long dataType_id;

    @DatabaseField(columnName = DATATYPE_CODE, canBeNull = false)
    @SerializedName(DATATYPE_CODE)
    @Expose
    private String dataType_code;

    @DatabaseField(columnName = DATATYPE_NAME, canBeNull = false)
    @SerializedName(DATATYPE_NAME)
    @Expose
    private String dataType_name;

    @DatabaseField(columnName = DATATYPE_DESCR, canBeNull = false)
    @SerializedName(DATATYPE_DESCR)
    @Expose
    private String dataType_descr;

    @DatabaseField(columnName = MODEL, canBeNull = false, defaultValue = "Static")
    @SerializedName(MODEL)
    @Expose
    private String model;

    @DatabaseField(columnName = STATUS, canBeNull = false ,defaultValue = "0")
    @SerializedName(STATUS)
    @Expose
    private String status;

    public Long getDataType_id() {
        return dataType_id;
    }

    public void setDataType_id(Long dataType_id) {
        this.dataType_id = dataType_id;
    }

    public String getDataType_code() {
        return dataType_code;
    }

    public void setDataType_code(String dataType_code) {
        this.dataType_code = dataType_code;
    }

    public String getDataType_name() {
        return dataType_name;
    }

    public void setDataType_name(String dataType_name) {
        this.dataType_name = dataType_name;
    }

    public String getDataType_descr() {
        return dataType_descr;
    }

    public void setDataType_descr(String dataType_descr) {
        this.dataType_descr = dataType_descr;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        return dataType_id.hashCode();
    }

    @Override
    public Long getIdentity() {
        return dataType_id;
    }

    @Override
    public String getIdentityAttribute() {
        return DATATYPE_ID;
    }
}
