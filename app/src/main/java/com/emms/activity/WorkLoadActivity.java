package com.emms.activity;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.datastore_android_sdk.datastore.DataElement;
import com.datastore_android_sdk.datastore.ObjectElement;
import com.datastore_android_sdk.rest.JsonArrayElement;
import com.datastore_android_sdk.rest.JsonObjectElement;
import com.datastore_android_sdk.rxvolley.client.HttpCallback;
import com.datastore_android_sdk.rxvolley.client.HttpParams;
import com.emms.R;
import com.emms.adapter.TaskAdapter;
import com.emms.adapter.WorkloadAdapter;
import com.emms.httputils.HttpUtils;
import com.emms.schema.Data;
import com.emms.schema.Task;
import com.emms.util.DataUtil;
import com.emms.util.ToastUtil;
import com.google.common.primitives.Floats;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.DuplicateFormatFlagsException;
import java.util.HashMap;

/**
 * Created by Administrator on 2016/7/29.
 */
public class WorkLoadActivity extends NfcActivity{
    private TextView group,task_id,total_worktime;
    private ListView list;
    private Button comfirm;
    private ObjectElement TaskDetail;
    private Context context=this;
    private WorkloadAdapter workloadAdapter;
    private ArrayList<ObjectElement> datas=new ArrayList<>();
    private HashMap<Integer,EditText> workloadMap=new HashMap<>();
    private boolean TaskComplete=false;
    private ArrayList<Integer> workloadKeylist=new ArrayList<Integer>();
 //   private HashMap<Integer,String> workloadEditTextNum=new HashMap<Integer, String>();
    private String TaskClass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workload);
        TaskDetail=new JsonObjectElement(getIntent().getStringExtra("TaskDetail"));
        TaskComplete=getIntent().getBooleanExtra("TaskComplete",false);
        TaskClass=getIntent().getStringExtra(Task.TASK_CLASS);
        initView();
        getWorkLoadFromServer();
    }
    private void initView(){
        if(TaskComplete){
            findViewById(R.id.footer_toolbar).setVisibility(View.VISIBLE);
            findViewById(R.id.comfirm).setVisibility(View.GONE);
            //initFooterToolBar
            findViewById(R.id.preStep).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            findViewById(R.id.nextStep).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //待写
                    submitWorkLoadToServer();
                }
            });
            if((!TaskClass.equals(Task.REPAIR_TASK))&&(!TaskClass.equals(Task.OTHER_TASK))){
                ((Button)findViewById(R.id.nextStep)).setText(R.string.taskComplete);
            }
        }
        workloadAdapter=new WorkloadAdapter(datas) {
            @Override
            public View getCustomView(View convertView, final int position, ViewGroup parent) {
                final WorkloadAdapter.ViewHolder holder;
               // if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_workload_activity, parent, false);
                    holder = new WorkloadAdapter.ViewHolder();
                    holder.name=(TextView)convertView.findViewById(R.id.name) ;
                    holder.skill=(TextView)convertView.findViewById(R.id.skill) ;
                    holder.startTime=(TextView)convertView.findViewById(R.id.start_time);
                   // holder.endTime=(TextView)convertView.findViewById(R.id.end_time) ;
                    holder.workload=(EditText)convertView.findViewById(R.id.workload) ;
                    holder.workload.setInputType(EditorInfo.TYPE_CLASS_PHONE);
                    convertView.setTag(holder);
//                } else {
//                    holder = (WorkloadAdapter.ViewHolder) convertView.getTag();
//                }
                holder.name.setText(DataUtil.isDataElementNull(datas.get(position).get("OperatorName")));
                holder.skill.setText(DataUtil.isDataElementNull(datas.get(position).get("Skill")));
                holder.startTime.setText(DataUtil.utc2Local(DataUtil.isDataElementNull(datas.get(position).get("StartTime"))));
                //holder.endTime.setText(DataUtil.getDate(DataUtil.isDataElementNull(datas.get(position).get("FinishTime"))));
                if(DataUtil.isFloat(DataUtil.isDataElementNull(datas.get(position).get("Coefficient")))){
                    if(  !(((int)(Float.parseFloat(DataUtil.isDataElementNull(datas.get(position).get("Coefficient")))*100))==0)    ){
                        holder.workload.setText(String.valueOf( (int)(Float.valueOf(DataUtil.isDataElementNull(datas.get(position).get("Coefficient"))) * 100)));
                        datas.get(position).set("Work",DataUtil.isDataElementNull(datas.get(position).get("Coefficient")));
                    }
                }
                if(datas.get(position).get("Work")!=null){
                if(DataUtil.isFloat(DataUtil.isDataElementNull(datas.get(position).get("Work")))){
                        holder.workload.setText(String.valueOf((int) (Float.valueOf(DataUtil.isDataElementNull(datas.get(position).get("Work"))) * 100)));
                }else {
                    holder.workload.setText(DataUtil.isDataElementNull(datas.get(position).get("Work")));
                }}
                workloadMap.put(position,holder.workload);
//               if(datas.get(position).get("Workload")!=null){
//                   holder.workload.setText(DataUtil.isDataElementNull(datas.get(position).get("Workload")));
//                }
//                if(workloadEditTextNum.get(position)!=null){
//                    holder.workload.setText(workloadEditTextNum.get(position));
//                }
                holder.workload.addTextChangedListener(new TextWatcher() {
                   @Override
                   public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                   }

                   @Override
                   public void onTextChanged(CharSequence s, int start, int before, int count) {
                          //if(s.toString().
                   }

                   @Override
                   public void afterTextChanged(Editable s) {
                       if(DataUtil.isInt(s.toString())){
                              datas.get(position).set("Work",Float.parseFloat(s.toString())/100.0f);
                       }else{
                           datas.get(position).set("Work",s.toString());
                       }
                      // workloadEditTextNum.put(position,holder.workload.getText().toString());
                   }
               });
                return convertView;
            }

        };
        group=(TextView)findViewById(R.id.group);
        task_id=(TextView)findViewById(R.id.task_ID);
        total_worktime=(TextView)findViewById(R.id.total_worktime);
        list=(ListView)findViewById(R.id.listView);
        list.setAdapter(workloadAdapter);

        comfirm=(Button)findViewById(R.id.comfirm);
        comfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitWorkLoadToServer();
            }
        });
        //initTopToolbar
        ((TextView)findViewById(R.id.tv_title)).setText(R.string.entering_workload);
        findViewById(R.id.btn_right_action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
    private void getWorkLoadFromServer(){
        showCustomDialog(R.string.loadingData);
        HttpParams httpParams=new HttpParams();
        httpParams.put("task_id", DataUtil.isDataElementNull(TaskDetail.get(Task.TASK_ID)));
        HttpUtils.get(this, "TaskWorkload", httpParams, new HttpCallback() {
            @Override
            public void onSuccess(final String t) {
                super.onSuccess(t);
                if(t!=null&&!t.equals("null")){
                   runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           JsonObjectElement jsonObjectElement=new JsonObjectElement(t);
                      SetViewData(jsonObjectElement);
                       }
                   });
                }else {
                    ToastUtil.showToastLong(R.string.loading_Fail,context);
                }
                dismissCustomDialog();
            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
                ToastUtil.showToastLong(R.string.loadingFail,context);
                dismissCustomDialog();
            }
        });
    }
    private void SetViewData(ObjectElement ViewData){
        group.setText(DataUtil.isDataElementNull(ViewData.get("TaskApplicantOrgName")));
        task_id.setText(DataUtil.isDataElementNull(ViewData.get(Task.TASK_ID)));
        total_worktime.setText(DataUtil.isDataElementNull(ViewData.get("Workload"))+getResources().getString(R.string.hours));
        if(ViewData.get("TaskOperator")!=null&&ViewData.get("TaskOperator").asArrayElement().size()>0) {
                for (int i = 0; i < ViewData.get("TaskOperator").asArrayElement().size(); i++) {
                    datas.add(ViewData.get("TaskOperator").asArrayElement().get(i).asObjectElement());
                }
            if(datas.size()==1){
                datas.get(0).set("Coefficient",1.0);
            }
            workloadAdapter.notifyDataSetChanged();
       /*     workloadAdapter.unregisterDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                }
            });*/

        }
    }
    private void submitWorkLoadToServer(){
        workloadKeylist.clear();
        workloadKeylist.addAll(workloadMap.keySet());

        int sum=0;
        for(int i=0;i<workloadKeylist.size();i++){
            if(workloadMap.get(workloadKeylist.get(i)).getText().toString().equals("")){
                ToastUtil.showToastLong(R.string.pleaseInputWorkload,this);
                return;
            }
            if(    !DataUtil.isNum(workloadMap.get(workloadKeylist.get(i)).getText().toString())
                   || !DataUtil.isInt(workloadMap.get(workloadKeylist.get(i)).getText().toString())
                   ||  Integer.parseInt(workloadMap.get(workloadKeylist.get(i)).getText().toString())<0 ){
                ToastUtil.showToastLong(R.string.pleaseInputInteger,this);
                return;
            }
            sum+=Integer.valueOf(workloadMap.get(workloadKeylist.get(i)).getText().toString());
        }
        if(sum!=100){
            ToastUtil.showToastLong(R.string.judgeWorkloadSum,this);
            return;
        }
        showCustomDialog(R.string.submitData);
        //if(workloadAdapter.)
      HttpParams httpParams=new HttpParams();
      ArrayList<ObjectElement> submitWorkloadData=new ArrayList<ObjectElement>();
      for (int i=0;i<workloadAdapter.getDatas().size();i++){
          ObjectElement obj=workloadAdapter.getDatas().get(i);
          JsonObjectElement jsonObjectElement=new JsonObjectElement();
          jsonObjectElement.set("TaskOperator_ID", DataUtil.isDataElementNull(obj.get("TaskOperator_ID")));
          jsonObjectElement.set("Coefficient",Float.valueOf(DataUtil.isDataElementNull(obj.get("Work"))));
          submitWorkloadData.add(jsonObjectElement);
      }
        JsonArrayElement submitData=new JsonArrayElement(submitWorkloadData.toString());
        httpParams.putJsonParams(submitData.toJson());
        HttpUtils.post(this, "TaskWorkload", httpParams, new HttpCallback() {
            @Override
            public void onSuccess(String t) {
                super.onSuccess(t);
                if(t!=null){
                    JsonObjectElement jsonObjectElement=new JsonObjectElement(t);
                    if(jsonObjectElement.get("Success")!=null&&jsonObjectElement.get("Success").valueAsBoolean()){
                        ToastUtil.showToastLong(R.string.submitSuccess,context);
                        dismissCustomDialog();
                        if(TaskComplete){
                            if(  TaskClass.equals(Task.REPAIR_TASK)  ||  TaskClass.equals(Task.OTHER_TASK)  ){
                                Intent intent=new Intent(context,SummaryActivity.class);
                                intent.putExtra("TaskComplete",true);
                                intent.putExtra(Task.TASK_CLASS,TaskClass);
                                intent.putExtra("TaskDetail",TaskDetail.toString());
                                startActivity(intent);}
                            else {
//                                Intent intent=new Intent(context,CommandActivity.class);
//                                intent.putExtra("TaskComplete",true);
//                                intent.putExtra("TaskDetail",TaskDetail.toString());
//                                startActivity(intent);
                                TaskComplete();
                            }
                        }else{
                            finish();
                        }
                    }else{
                        ToastUtil.showToastLong(R.string.workloadSubmitFail,context);
                    }
                }

            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
                dismissCustomDialog();
                ToastUtil.showToastLong(R.string.submitFail,context);
            }
        });

    }

    @Override
    public void resolveNfcMessage(Intent intent) {

    }
    private void TaskComplete(){
        showCustomDialog(R.string.submitData);
        HttpParams params=new HttpParams();
        JsonObjectElement data=new JsonObjectElement();
        data.set(Task.TASK_ID,task_id.getText().toString());
        params.putJsonParams(data.toJson());
        HttpUtils.post(this, "TaskFinish", params, new HttpCallback() {
            @Override
            public void onSuccess(String t) {
                if(t!=null){
                    JsonObjectElement jsonObjectElement=new JsonObjectElement(t);
                    if(jsonObjectElement.get("Success")!=null&&
                            jsonObjectElement.get("Success").valueAsBoolean()){
                        ToastUtil.showToastLong(R.string.taskComplete,context);
                        startActivity(new Intent(context,CusActivity.class));
                    }else {
                        ToastUtil.showToastLong(R.string.canNotSubmitTaskComplete,context);
                    }
                }
            dismissCustomDialog();
            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
                dismissCustomDialog();
                ToastUtil.showToastLong(R.string.canNotSubmitTaskCompleteCauseByTimeOut,context);
            }
        });
    }
}
