package com.emms.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.datastore_android_sdk.datastore.DataElement;
import com.datastore_android_sdk.datastore.ObjectElement;
import com.datastore_android_sdk.rest.JsonObjectElement;
import com.datastore_android_sdk.rxvolley.client.HttpCallback;
import com.datastore_android_sdk.rxvolley.client.HttpParams;
import com.datastore_android_sdk.sqlite.SqliteStore;
import com.emms.R;
import com.emms.activity.AppAplication;
import com.emms.activity.TaskDetailsActivity;
import com.emms.adapter.StatusAdapter;
import com.emms.adapter.TaskAdapter;
import com.emms.bean.WorkInfo;
import com.emms.datastore.EPassSqliteStoreOpenHelper;
import com.emms.httputils.HttpUtils;
import com.emms.schema.DataDictionary;
import com.emms.schema.Task;
import com.emms.util.DataUtil;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义对话框
 * Created by laomingfeng on 2016/5/24.
 */
public class ChangeEquipmentDialog extends Dialog implements View.OnClickListener{
    private ChangeEquipmentDialog dialog = this;
    private Context context;
    private String TaskId;
    private String EquipmentId;
    private String TaskEquipmentId;
    //private ArrayList<String> status=new ArrayList<String>();
   private Button change_equipment_operator_status,change_equipment_status;
    private Button equipment_operator_resume,equipment_operator_complete,equipment_operator_wait_material,equipment_operator_material_requisition,equipment_operator_quit,
            equipment_operator_pause,equipment_operator_start;
    private Button equipment_resume,equipment_complete,equipment_wait_material,equipment_pause,equipment_start;
    private ObjectElement TaskEquipmentData;
    public ChangeEquipmentDialog(Context context, int layout, int style) {
        super(context, style);
        this.context = context;
        setContentView(layout);
      //  Collections.addAll(status,context.getResources().getStringArray(R.array.equip_status));
        initview();
    }

    public void initview() {
        ((TextView)findViewById(R.id.cancle)).setText(R.string.cancel);
        findViewById(R.id.cancle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        initEquipmentOperatorTagView();
        initEquipmentTagView();
    }



    public void submitSubTaskData() {
        HttpParams params = new HttpParams();
        JsonObjectElement jsonObjectElement = new JsonObjectElement();

        HttpUtils.post(context, "TaskItem", params, new HttpCallback() {
            @Override
            public void onSuccess(String t) {
                super.onSuccess(t);
            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
            }
        });
    }
    private void postTaskEquipment(int status) {

        HttpParams params = new HttpParams();

        JsonObjectElement taskEquepment=new JsonObjectElement();
//创建任务提交数据：任务创建人，任务类型“T01”那些，几台号（数组），
        taskEquepment.set(Task.TASK_ID,TaskId);
        //若任务未有设备，则输入为0，表示添加
        taskEquepment.set("TaskEquipment_ID",TaskEquipmentId);
        //若已有设备，申请状态变更
        taskEquepment.set("AssetsID", EquipmentId);
        //taskEquepment.set("Equipment_ID", equipmentID);
        taskEquepment.set("Status",status);

        params.putJsonParams(taskEquepment.toJson());

        HttpUtils.post(context, "TaskEquipment", params, new HttpCallback() {
            @Override
            public void onSuccess(String t) {
                super.onSuccess(t);
                Toast.makeText(context, "修改设备状态成功", Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
                        Toast.makeText(context, "修改设备状态失败", Toast.LENGTH_SHORT).show();

            }
        });
    }

    public ObjectElement getTaskEquipmentData() {
        return TaskEquipmentData;
    }

    public void setTaskEquipmentData(ObjectElement taskEquipmentData) {
        TaskEquipmentData = taskEquipmentData;
    }
   public void setDatas(String taskId,String equipmentId,String taskEquipmentId){
       this.TaskId=taskId;
       this.EquipmentId=equipmentId;
       this.TaskEquipmentId=taskEquipmentId;
   }
    private void initEquipmentOperatorTagView(){
        equipment_operator_resume=(Button)findViewById(R.id.equipment_resume);
        equipment_operator_complete=(Button)findViewById(R.id.equipment_resume);
        equipment_operator_wait_material=(Button)findViewById(R.id.equipment_resume);
        equipment_operator_material_requisition=(Button)findViewById(R.id.equipment_resume);
        equipment_operator_quit=(Button)findViewById(R.id.equipment_resume);
        equipment_operator_pause=(Button)findViewById(R.id.equipment_resume);
        equipment_operator_start=(Button)findViewById(R.id.equipment_resume);
    }
    private void initEquipmentTagView(){
        equipment_resume=(Button)findViewById(R.id.equipment_resume);
        equipment_complete=(Button)findViewById(R.id.equipment_complete);
        equipment_wait_material=(Button)findViewById(R.id.equipment_wait_material);
        equipment_pause=(Button)findViewById(R.id.equipment_pause);
        equipment_start=(Button)findViewById(R.id.equipment_start);
    }

    @Override
    public void onClick(View v) {
        int id=v.getId();
        switch (id){
            case R.id.equipment_resume:{
                postTaskEquipment(1);
                break;
            }
            case R.id.equipment_complete:{
                postTaskEquipment(4);
                break;
            }
            case R.id.equipment_wait_material:{
                postTaskEquipment(3);
                break;
            }
            case R.id.equipment_pause:{
                postTaskEquipment(2);
                break;
            }
            case R.id.equipment_start:{
                postTaskEquipment(1);
                break;
            }
        }
    }
}