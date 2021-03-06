package com.emms.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.datastore_android_sdk.DatastoreException.DatastoreException;
import com.datastore_android_sdk.callback.StoreCallback;
import com.datastore_android_sdk.datastore.DataElement;
import com.datastore_android_sdk.datastore.ObjectElement;
import com.datastore_android_sdk.rest.JsonArrayElement;
import com.datastore_android_sdk.rest.JsonObjectElement;
import com.datastore_android_sdk.rxvolley.client.HttpCallback;
import com.datastore_android_sdk.rxvolley.client.HttpParams;
import com.emms.R;
import com.emms.adapter.MainActivityAdapter;
import com.emms.datastore.EPassSqliteStoreOpenHelper;
import com.emms.httputils.HttpUtils;
import com.emms.schema.Factory;
import com.emms.schema.Task;
import com.emms.util.BaseData;
import com.emms.util.BuildConfig;
import com.emms.util.DataUtil;
import com.emms.util.NetworkUtils;
import com.emms.util.ServiceUtils;
import com.emms.util.SharedPreferenceManager;
import com.emms.util.ToastUtil;
import com.flyco.tablayout.widget.MsgView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.tencent.bugly.crashreport.CrashReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import cn.jpush.android.api.JPushInterface;

/**
 * Created by Administrator on 2016/8/23.
 *
 */
public class CusActivity extends NfcActivity implements View.OnClickListener{
    private Context context=this;
    private ArrayList<ObjectElement> moduleList=new ArrayList<>();
    private static HashMap<String,Integer> TaskClass_moduleID_map=new HashMap<>();
    private HashMap<Integer,ObjectElement> ID_module_map=new HashMap<>();
    private MainActivityAdapter adapter;
    RefreshTaskNumBroadCast refreshTaskNumBroadCast=new RefreshTaskNumBroadCast();
    IntentFilter intentFilter=new IntentFilter("RefreshTaskNum");
    private static boolean syncLock=false;//用于限制只能同时进行一次任务数字获取
    private int retryTimes=1;
    private Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cus);
        mHandler=new Handler(getMainLooper());
        AppApplication.KeepLive=true;
        ServiceUtils.starKeepLiveService(ServiceUtils.Mode.Only_KeepLiveServiceNo_1,this);
//        BroadcastUtils.startKeepLiveBroadcast(this);
        if(Factory.FACTORY_EGM.equals(SharedPreferenceManager.getFactory(this))) {
            AppApplication.AppTimeZone="GMT+8";
//            TimeZone.setDefault(TimeZone.getTimeZone("GMT+7"));
        }else {
            AppApplication.AppTimeZone="GMT+8";
//            TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
        }
        {
            TaskClass_moduleID_map.put(Task.REPAIR_TASK,3);//车间报修
            TaskClass_moduleID_map.put(Task.GROUP_ARRANGEMENT,2);//组内安排
            TaskClass_moduleID_map.put(Task.MOVE_CAR_TASK,4);//搬车任务
            TaskClass_moduleID_map.put(Task.OTHER_TASK,5);//其它任务
            TaskClass_moduleID_map.put(Task.ROUTING_INSPECTION,11);//点巡检
            TaskClass_moduleID_map.put(Task.UPKEEP,12);//保养
            TaskClass_moduleID_map.put(Task.MOVE_CAR_TASK,13);//搬车任务
            TaskClass_moduleID_map.put(Task.TRANSFER_MODEL_TASK,14);//调车任务
            TaskClass_moduleID_map.put("C2",8); //工时审核
            TaskClass_moduleID_map.put("C1",10);//任务审核
            TaskClass_moduleID_map.put("C3",7); //任务历史
        }
        String sql="select (case when dd.[DataValue3]='Garment' then 'EGM' else dd.[DataValue3] end) appMode from DataDictionary  dd where dd.DataType = 'AppFactorySetting'"
                +" and dd.[Factory_ID] ='"+SharedPreferenceManager.getFactory(context)+"'";
        getSqliteStore().performRawQuery(sql, EPassSqliteStoreOpenHelper.SCHEMA_DATADICTIONARY, new StoreCallback() {
            @Override
            public void success(DataElement element, String resource) {
                if(element!=null&&element.isArray()&&element.asArrayElement().size()>0){
                        SharedPreferenceManager.setAppMode(context,DataUtil.isDataElementNull(element.asArrayElement().get(0).asObjectElement().get("appMode")));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initView();
                        initData();
                        getTaskCountFromServer(true);
                    }
                });
            }

            @Override
            public void failure(DatastoreException ex, String resource) {
                  Log.e("fail",ex.toString());
            }
        });


    }

    @Override
    public void resolveNfcMessage(Intent intent) {

    }
    private void initView(){
        Button btn_exit = (Button) findViewById(R.id.btn_exit);
        btn_exit.setOnClickListener(this);
        if(getLoginInfo()!=null) {
            ((TextView) findViewById(R.id.UserName)).setText(getLoginInfo().getName());
            ((TextView) findViewById(R.id.WorkNum_tag)).setText(getLoginInfo().getOperator_no());
        }
        final PullToRefreshGridView module_list = (PullToRefreshGridView) findViewById(R.id.module_list);
        adapter=new MainActivityAdapter(moduleList) {
            @Override
            public View getCustomView(View convertView, int position, ViewGroup parent) {
                MainActivityAdapter.TaskViewHolder holder;
//                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_activity_cur, parent, false);
                    holder = new MainActivityAdapter.TaskViewHolder();
                    holder.image=(ImageView)convertView.findViewById(R.id.module_image);
                    holder.moduleName=(TextView)convertView.findViewById(R.id.module_name);
                    holder.msgView=(MsgView)convertView.findViewById(R.id.task_num);
                    convertView.setTag(holder);
//                } else {
//                    holder = (MainActivityAdapter.TaskViewHolder) convertView.getTag();
//                }
                if(moduleList.get(position).get("module_image")!=null){
                holder.image.setImageResource(moduleList.get(position).get("module_image").valueAsInt());}
                if(moduleList.get(position).get("module_name")!=null){
                holder.moduleName.setText(moduleList.get(position).get("module_name").valueAsInt());}
                if(moduleList.get(position).get("TaskNum")!=null){
                    holder.msgView.setVisibility(View.VISIBLE);
                    switch (moduleList.get(position).get("TaskNumType").valueAsInt()){
                        case 0: {
                        break;
                        }
                        case 1:{
                            String s[]=DataUtil.isDataElementNull(moduleList.get(position).get("TaskNum")).split("/");
                            if(DataUtil.isInt(s[1])&&Integer.valueOf(s[1])==0){
                                holder.msgView.setBgSelector2();
                            }
                            break;
                        }case 2:{
                            if(DataUtil.isInt(DataUtil.isDataElementNull(moduleList.get(position).get("TaskNum")))
                                    &&moduleList.get(position).get("TaskNum").valueAsInt()==0){
                                holder.msgView.setBgSelector2();
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                    holder.msgView.setText(DataUtil.isDataElementNull(moduleList.get(position).get("TaskNum")));
                }
                return convertView;
            }
        };
        module_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(moduleList.get(position).get("Class")!=null){
                    try {
                        Class c=Class.forName(DataUtil.isDataElementNull(moduleList.get(position).get("Class")));
                        Intent intent=new Intent(context,c);
                        if(moduleList.get(position).get(Task.TASK_CLASS)!=null){
                            intent.putExtra(Task.TASK_CLASS,DataUtil.isDataElementNull(moduleList.get(position).get(Task.TASK_CLASS)));
                        }
                        if(moduleList.get(position).get("TaskNum")!=null){
                            intent.putExtra("TaskNum",DataUtil.isDataElementNull(moduleList.get(position).get("TaskNum")));
                        }
                        if(moduleList.get(position).get(Task.TASK_SUBCLASS)!=null){
                            intent.putExtra(Task.TASK_SUBCLASS,DataUtil.isDataElementNull(moduleList.get(position).get(Task.TASK_SUBCLASS)));
                        }
                        startActivity(intent);
                    }catch (Throwable e){
                        CrashReport.postCatchedException(e);
                    }
                }
            }
        });
        module_list.setAdapter(adapter);
        module_list.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
        module_list.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<GridView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<GridView> refreshView) {
                getTaskCountFromServer(true);
                module_list.onRefreshComplete();
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<GridView> refreshView) {

            }
        });

        //TODO
        if(SharedPreferenceManager.getUserRoleID(this)!=null&&Integer.valueOf(SharedPreferenceManager.getUserRoleID(this))!=null) {
            if (Integer.valueOf(SharedPreferenceManager.getUserRoleID(this)) == 7) {
                ((ImageView) findViewById(R.id.rootImage)).setImageResource(R.mipmap.applicant);
            } else if (Integer.valueOf(SharedPreferenceManager.getUserRoleID(this)) < 5) {
                ((ImageView) findViewById(R.id.rootImage)).setImageResource(R.mipmap.repairerleader);
            } else {
                ((ImageView) findViewById(R.id.rootImage)).setImageResource(R.mipmap.repairer);
            }
        }else {
            ((ImageView) findViewById(R.id.rootImage)).setImageResource(R.mipmap.repairer);
        }
    }
    private void initData(){
        if(getIntent().getStringExtra("Module_ID_List")!=null&&!getIntent().getStringExtra("Module_ID_List").equals("")) {
            String module = getIntent().getStringExtra("Module_ID_List");
            String[] modules = module.split(",");
            for (String module1 : modules) {
                JsonObjectElement jsonObjectElement = new JsonObjectElement();
                jsonObjectElement.set("module_ID", Integer.valueOf(module1));
                jsonObjectElement = moduleMatchingRule(jsonObjectElement);
                ID_module_map.put(Integer.valueOf(module1), jsonObjectElement);
                moduleList.add(jsonObjectElement);
            }
        }else {
            if(!SharedPreferenceManager.getUserModuleList(this).equals("")){
                String module = SharedPreferenceManager.getUserModuleList(this);
                String[] modules = module.split(",");
                for (String module1 : modules) {
                    JsonObjectElement jsonObjectElement = new JsonObjectElement();
                    jsonObjectElement.set("module_ID", Integer.valueOf(module1));
                    jsonObjectElement = moduleMatchingRule(jsonObjectElement);
                    ID_module_map.put(Integer.valueOf(module1), jsonObjectElement);
                    moduleList.add(jsonObjectElement);
                }
            }else {
                for (int i = 0; i < 10; i++) {
                    JsonObjectElement jsonObjectElement = new JsonObjectElement();
                    jsonObjectElement.set("module_ID", i + 1);
                    jsonObjectElement = moduleMatchingRule(jsonObjectElement);
                    ID_module_map.put(i + 1, jsonObjectElement);
                    moduleList.add(jsonObjectElement);
                }
            }
        }

    }
    //个性化开发，根据服务器返回的角色模块ID进行个性化配置
    private JsonObjectElement moduleMatchingRule(JsonObjectElement obj){
         int module_id=obj.get("module_ID").valueAsInt();
        switch (module_id){
            case 1:{//createTask
                setModelProperty(obj,R.mipmap.cur_activity_create_task,
                        R.string.create_task,null,null,"CreateTaskActivity",null,0);
                break;
            }
            case 2:{//maintainTask
                setModelProperty(obj,R.mipmap.cur_activity_maintain,
                        R.string.GroupArrangement,Task.GROUP_ARRANGEMENT,null,"TaskListActivity","0/0",1);
                break;
            }
            case 3:{//repairTask
                setModelProperty(obj,R.mipmap.cur_activity_repair,
                        R.string.repair,Task.REPAIR_TASK,null,"TaskListActivity","0/0",1);
                break;
            }
            case 4:{//moveCarTask
                setModelProperty(obj,R.mipmap.cur_activity_move_car,
                        R.string.move_car,Task.MOVE_CAR_TASK,null,"TaskListActivity","0/0",1);
                break;
            }
            case 5:{//teamStatus
                setModelProperty(obj,R.mipmap.cur_activity_other,
                        R.string.other,Task.OTHER_TASK,null,"TaskListActivity","0/0",1);
                break;
            }
            case 6:{//deviceFaultSummary
                setModelProperty(obj,R.mipmap.cur_activity_equipment_summary,
                        R.string.DeveceHistory,null,null,"EquipmentHistory",null,0);
                break;
            }
            case 7:{//TaskCommand
                setModelProperty(obj,R.mipmap.cur_activity_task_history,
                        R.string.taskHistory,null,null,"TaskHistoryCheck","0",2);
                break;
            }
            case 8:{//workloadverify
                setModelProperty(obj,R.mipmap.cur_activity_workload_verify,
                        R.string.workloadVerify,null,null,"WorkloadVerifyActivity","0",2);
                break;
            }
            case 9:{//team staff
                setModelProperty(obj,R.mipmap.cur_activity_team,
                        R.string.team,null,null,"TeamStatusActivity",null,0);
                break;
            }
            case 10:{//taskverify
                setModelProperty(obj,R.mipmap.cur_activity_verify,
                        R.string.TaskVerify,null,null,"TaskVerifyActivity","0",2);
                break;
            }
            case 11:{//巡检
                setModelProperty(obj,R.mipmap.module_measure_point,
                        R.string.routingInspection,Task.MAINTAIN_TASK,Task.ROUTING_INSPECTION, "TaskListActivity","0/0",1);
                break;
            }
            case 12:{//保养
                setModelProperty(obj,R.mipmap.module_upkeep,
                        R.string.upkeep,Task.MAINTAIN_TASK,Task.UPKEEP,"TaskListActivity","0/0",1);
                break;
            }
            case 13:{//搬车
                setModelProperty(obj,R.mipmap.cur_activity_move_car,
                        R.string.move_car,Task.MOVE_CAR_TASK,null,"TaskListActivity","0/0",1);
                break;
            }
            case 14: {//转款
                setModelProperty(obj, R.mipmap.model_transfer_model,
                        R.string.transfer_model, Task.TRANSFER_MODEL_TASK, null, "TaskListActivity", "0/0", 1);
                break;
            }
            case 15:{
                setModelProperty(obj,R.mipmap.system_setting_activity_binding,
                        R.string.EquipmentBinding,null,null,"EnteringEquipmentICCardIDActivity",null,0);
                break;
            }
            default:{
                setModelProperty(obj, R.mipmap.model_transfer_model,
                        R.string.transfer_model, Task.TRANSFER_MODEL_TASK, null, "TaskListActivity", "0/0", 1);
            }
        }
        return obj;
    }
    private void setModelProperty(JsonObjectElement obj,int module_image,int module_name,String TaskClass,String TaskSubClass,
                                  String Class,String TaskNum,int TaskNumType){
        String packageName="com.emms.activity.";
        obj.set("module_image",module_image);
        obj.set("module_name",module_name);
        if(TaskClass!=null) {
            obj.set(Task.TASK_CLASS, TaskClass);
        }
        if(TaskSubClass!=null) {
            obj.set(Task.TASK_SUBCLASS, TaskSubClass);
        }
        obj.set("Class",packageName+Class);
        if(TaskNum!=null) {
            obj.set("TaskNum", TaskNum);
        }
        obj.set("TaskNumType",TaskNumType);
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        BuildConfig.NetWorkSetting(context);
        getTaskCountFromServer(true);
        getDBDataLastUpdateTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (refreshTaskNumBroadCast != null) {
                context.registerReceiver(refreshTaskNumBroadCast, intentFilter);
            }
        }catch (Exception e){
            CrashReport.postCatchedException(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (refreshTaskNumBroadCast != null) {
                unregisterReceiver(refreshTaskNumBroadCast);
            }
        }catch (Exception e){
            CrashReport.postCatchedException(e);
        }
    }

    //获取任务数量
    private synchronized void getTaskCountFromServer(boolean showLoadingDialog){
        if(syncLock){
            return;
        }
        syncLock=true;
        if(showLoadingDialog) {
            showCustomDialog(R.string.loadingData);
        }
        HttpParams params=new HttpParams();
        //params.put("id",String.valueOf(getLoginInfo().getId()));
        // String s=SharedPreferenceManager.getUserName(this);
        HttpUtils.get(this, "TaskAPI/TaskNum", params, new HttpCallback() {
            @Override
            public void onSuccess(String t) {
                super.onSuccess(t);
                retryTimes=1;
                syncLock=false;
                try {
                    if (t != null) {
                        JsonArrayElement json = new JsonArrayElement(t);
                        //获取任务数目，Data_ID对应，1对应维修，2对应维护，3对应搬车，4对应其它
                        for (int i = 0; i < json.size(); i++) {
                            //   taskNum.put(jsonObjectElement.get("PageData").asArrayElement().get(i).asObjectElement().get("Data_ID").valueAsInt(),
                            //         jsonObjectElement.get("PageData").asArrayElement().get(i).asObjectElement());
                            if (json.get(i).asObjectElement().get("DoingNo") != null &&
                                    json.get(i).asObjectElement().get("ToDoNo") != null) {
                                ObjectElement jsonObjectElement = json.get(i).asObjectElement();
                                optimizationData(jsonObjectElement, "ToDoNo", "DoingNo");
                                String taskNumToShow;
                                if (DataUtil.isDataElementNull(jsonObjectElement.get("TaskClass")).equals("C1")
                                        || DataUtil.isDataElementNull(jsonObjectElement.get("TaskClass")).equals("C2")
                                        || DataUtil.isDataElementNull(jsonObjectElement.get("TaskClass")).equals("C3")) {
                                    taskNumToShow = DataUtil.isDataElementNull(jsonObjectElement.get("ToDoNo"));
                                } else {
                                    taskNumToShow = DataUtil.isDataElementNull(jsonObjectElement.get("DoingNo")) + "/" +
                                            DataUtil.isDataElementNull(jsonObjectElement.get("ToDoNo"));
                                }
                                if (ID_module_map.get(TaskClass_moduleID_map.get(DataUtil.isDataElementNull(jsonObjectElement.get("TaskClass")))) != null) {
                                    ID_module_map.get(TaskClass_moduleID_map.get(DataUtil.isDataElementNull(jsonObjectElement.get("TaskClass")))).set("TaskNum", taskNumToShow);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }catch (Exception e){
                    CrashReport.postCatchedException(e);
                }
                BaseData.setBaseData(context);
                dismissCustomDialog();
            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
                syncLock=false;
                try {
                    BaseData.setBaseData(context);
                    if (errorNo == 401) {
                        ToastUtil.showToastShort(R.string.unauthorization, context);
                        dismissCustomDialog();
                        return;
                    }
                    if(retryTimes<4){
                        if(mHandler!=null){
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e("retry",String.valueOf(retryTimes));
                                    retryTimes++;
                                    getTaskCountFromServer(true);
                                }
                            },1500);
                        }else {
                            mHandler=new Handler(getMainLooper());
                        }
                        return;
                    }
                    ToastUtil.showToastShort(getString(R.string.loadingFail)+"\n"+strMsg+"\n"+"returnCode:"+errorNo, context);
                }catch (Exception e){
                    //Do nothing
                    dismissCustomDialog();
                }
                dismissCustomDialog();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_exit) {
            showCustomDialog(R.string.logout);
            HttpParams params = new HttpParams();
            HttpUtils.delete(this, "Token", params, new HttpCallback() {
                @Override
                public void onSuccess(String t) {
                    super.onSuccess(t);
                    dismissCustomDialog();
                    if(!JPushInterface.isPushStopped(context)){
                    JPushInterface.stopPush(context);}
                    logout();
                }

                @Override
                public void onFailure(int errorNo, String strMsg) {
                    super.onFailure(errorNo, strMsg);
                    dismissCustomDialog();
                    if(!JPushInterface.isPushStopped(context)) {
                        JPushInterface.stopPush(context);
                    }
                    logout();
                }
            });
        }
    }

    /**
     * When the User Logout ,clear all the User Info from SharePreference except Account
     */
    private void logout(){
        SharedPreferenceManager.setPassWord(CusActivity.this,null);
        SharedPreferenceManager.setCookie(CusActivity.this,null);
        SharedPreferenceManager.setLoginData(CusActivity.this,null);
        SharedPreferenceManager.setUserData(CusActivity.this,null);
        SharedPreferenceManager.setMsg(CusActivity.this,null);
        SharedPreferenceManager.setUserRoleID(CusActivity.this,null);
        Intent intent=new Intent(CusActivity.this, LoginActivity.class);
        intent.putExtra("FromCusActivity",true);
        startActivity(intent);
    }
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }
    private void optimizationData(ObjectElement data,String key1,String key2){
        if(data.get(key1)!=null
                &&DataUtil.isNum(DataUtil.isDataElementNull(data.get(key1)))
                &&data.get(key1).valueAsInt()>=100){
            data.set(key1,"99");
        }
        if(data.get(key2)!=null
                &&DataUtil.isNum(DataUtil.isDataElementNull(data.get(key2)))
                &&data.get(key2).valueAsInt()>=100){
            data.set(key2,"99");
        }
    }

    private class RefreshTaskNumBroadCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
              if("RefreshTaskNum".equals(intent.getAction())){
                  getTaskCountFromServer(false);
              }
        }
    }
}
