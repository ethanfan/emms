package com.emms.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.datastore_android_sdk.datastore.ObjectElement;
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

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/7/29.
 */
public class WorkLoadActivity extends BaseActivity{
    private TextView group,task_id,total_worktime;
    private ListView list;
    private RelativeLayout comfirm;
    private ObjectElement TaskDetail;
    private Context context=this;
    private WorkloadAdapter workloadAdapter;
    private ArrayList<ObjectElement> datas=new ArrayList<ObjectElement>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workload);
        TaskDetail=new JsonObjectElement(getIntent().getStringExtra("TaskDetail"));
        getWorkLoadFromServer();
        initView();

    }
    private void initView(){
        workloadAdapter=new WorkloadAdapter(datas) {
            @Override
            public View getCustomView(View convertView, int position, ViewGroup parent) {
                WorkloadAdapter.ViewHolder holder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_workload_activity, parent, false);
                    holder = new WorkloadAdapter.ViewHolder();
                    holder.name=(TextView)findViewById(R.id.name) ;
                    holder.skill=(TextView)findViewById(R.id.skill) ;
                    holder.startTime=(TextView)findViewById(R.id.start_time) ;
                    holder.endTime=(TextView)findViewById(R.id.end_time) ;
                    holder.workload=(EditText)findViewById(R.id.workload) ;
                    convertView.setTag(holder);
                } else {
                    holder = (WorkloadAdapter.ViewHolder) convertView.getTag();
                }
                holder.name.setText(DataUtil.isDataElementNull(datas.get(position).get("name")));
                holder.skill.setText(DataUtil.isDataElementNull(datas.get(position).get("Skill")));
                holder.startTime.setText(DataUtil.isDataElementNull(datas.get(position).get("StartTime")));
                holder.endTime.setText(DataUtil.isDataElementNull(datas.get(position).get("FinishTime")));
                holder.workload.setText(DataUtil.isDataElementNull(datas.get(position).get("Workload"))+getResources().getString(R.string.hours));
                return convertView;
            }

        };
        group=(TextView)findViewById(R.id.group);
        task_id=(TextView)findViewById(R.id.task_ID);
        total_worktime=(TextView)findViewById(R.id.total_worktime);
        list=(ListView)findViewById(R.id.listView);
        list.setAdapter(workloadAdapter);
        comfirm=(RelativeLayout)findViewById(R.id.comfirm);
        comfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitWorkLoadToServer();
            }
        });
    }
    private void getWorkLoadFromServer(){
        HttpParams httpParams=new HttpParams();
        httpParams.put("task_id", DataUtil.isDataElementNull(TaskDetail.get(Task.TASK_ID)));
        HttpUtils.get(this, "TaskWorkload", httpParams, new HttpCallback() {
            @Override
            public void onSuccess(final String t) {
                super.onSuccess(t);
                if(t!=null){
                   runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                      SetViewData(t);
                       }
                   });
                }
            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
            }
        });
    }
    private void SetViewData(String t){
        JsonObjectElement ViewData=new JsonObjectElement(t);
        group.setText(DataUtil.isDataElementNull(ViewData.get("TaskApplicantOrg")));
        task_id.setText(DataUtil.isDataElementNull(ViewData.get(Task.TASK_ID)));
        total_worktime.setText(DataUtil.isDataElementNull(ViewData.get("Workload"))+getResources().getString(R.string.hours));
        if(ViewData.get("TaskOperator")!=null&&ViewData.get("TaskOperator").asArrayElement().size()>0){
            for(int i=0;i<ViewData.get("TaskOperator").asArrayElement().size();i++){
                datas.add(ViewData.get("TaskOperator").asArrayElement().get(i).asObjectElement());
            }
            workloadAdapter.notifyDataSetChanged();
        }
    }
    private void submitWorkLoadToServer(){
      HttpParams httpParams=new HttpParams();
      ArrayList<ObjectElement> submitWorkloadData=new ArrayList<ObjectElement>();
      for (int i=0;i<workloadAdapter.getDatas().size();i++){
          JsonObjectElement jsonObjectElement=new JsonObjectElement();
          jsonObjectElement.set("TaskOperator_ID", DataUtil.isDataElementNull(workloadAdapter.getDatas().get(i).get("TaskOperator_ID")));

      }
    }
}
