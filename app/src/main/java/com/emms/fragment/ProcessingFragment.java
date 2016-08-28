package com.emms.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.datastore_android_sdk.datastore.ObjectElement;
import com.datastore_android_sdk.rest.JsonObjectElement;
import com.datastore_android_sdk.rxvolley.client.HttpCallback;
import com.datastore_android_sdk.rxvolley.client.HttpParams;
import com.emms.R;
import com.emms.activity.TaskDetailsActivity;
import com.emms.activity.TaskNumInteface;
import com.emms.adapter.TaskAdapter;
import com.emms.httputils.HttpUtils;
import com.emms.schema.Task;
import com.emms.ui.CancelTaskDialog;
import com.emms.ui.TaskCancelListener;
import com.emms.util.Constants;
import com.emms.util.DataUtil;
import com.emms.util.ToastUtil;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.util.ArrayList;

/**
 * Created by jaffer.deng on 2016/6/20.
 */
public class ProcessingFragment extends BaseFragment {

    private PullToRefreshListView listView;
    private TaskAdapter taskAdapter;
    private ProcessingFragment processingFragment=this;
    public ArrayList<ObjectElement> getDatas() {
        return datas;
    }

  //  public void setData(ArrayList<ObjectElement> objectElements){
   //     listView.setAdapter(taskAdapter);
  //      taskAdapter.setDatas(objectElements);
  //  }
    //private ArrayList<TaskBean> datas;

    private ArrayList<ObjectElement> datas=new ArrayList<ObjectElement>();
    private Context mContext;
    private Handler handler=new Handler();
    private String TaskClass;
    private  int PAGE_SIZE=10;
    private int pageIndex=1;
    private int RecCount=0;
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        TaskClass=this.getArguments().getString(Task.TASK_CLASS);
        mContext =getActivity();
        View v = inflater.inflate(R.layout.fr_processing, null);
        listView = (PullToRefreshListView) v.findViewById(R.id.processing_list);
       // listView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
        listView.setMode(PullToRefreshBase.Mode.BOTH);
       listView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                //上拉加载更多
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pageIndex=1;
                        getProcessingDataFromServer();
                        listView.onRefreshComplete();
                      //  Toast.makeText(mContext,"获取数据成功",Toast.LENGTH_SHORT).show();
                    }
                },0);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getProcessingDataFromServer();
                        listView.onRefreshComplete();
                        //Toast.makeText(mContext,"dada",Toast.LENGTH_SHORT).show();
                    }
                },0);
            }
        });
        taskAdapter = new TaskAdapter(datas) {
            @Override
            public View getCustomView(View convertView, int position, ViewGroup parent) {
                TaskViewHolder holder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.item_fr_process, parent, false);
                    holder = new TaskViewHolder();
                    //显示6个内容，组别，报修人，状态，保修时间,开始时间，任务描述
                    convertView.findViewById(R.id.id_end_time_description).setVisibility(View.GONE);
                    convertView.findViewById(R.id.tv_end_time_process).setVisibility(View.GONE);
                    holder.tv_group = (TextView) convertView.findViewById(R.id.group);
                    holder.warranty_person=(TextView)convertView.findViewById(R.id.Warranty_person);
                    //holder.tv_task_state = (TextView) convertView.findViewById(R.id.tv_task_state);
                    holder.tv_repair_time=(TextView)convertView.findViewById(R.id.tv_Warranty_time_process);
                    holder.tv_start_time = (TextView) convertView.findViewById(R.id.tv_start_time_process);
                    holder.tv_task_describe = (TextView) convertView.findViewById(R.id.tv_task_describe);
                    convertView.setTag(holder);
                } else {
                    holder = (TaskViewHolder) convertView.getTag();
                }
                //待修改
               holder.tv_group.setText(DataUtil.isDataElementNull(datas.get(position).get(Task.ORGANISE_NAME)));
                holder.warranty_person.setText(DataUtil.isDataElementNull(datas.get(position).get(Task.APPLICANT)));
            //    holder.tv_task_state.setText(DataUtil.isDataElementNull(datas.get(position).get(Task.TASK_STATUS)));
                holder.tv_repair_time.setText(DataUtil.getDate(DataUtil.isDataElementNull(datas.get(position).get(Task.APPLICANT_TIME))));
                holder.tv_start_time.setText(DataUtil.getDate(DataUtil.isDataElementNull(datas.get(position).get(Task.START_TIME))));
                holder.tv_task_describe.setText(DataUtil.isDataElementNull(datas.get(position).get(Task.TASK_DESCRIPTION)));
                return convertView;
            }
        };
       listView.setAdapter(taskAdapter);
        getProcessingDataFromServer();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(mContext,TaskDetailsActivity.class);
                intent.putExtra(Task.TASK_ID,datas.get(position-1).get(Task.TASK_ID).valueAsString());
                intent.putExtra("TaskDetail",datas.get(position-1).toString());
                intent.putExtra(Task.TASK_CLASS,TaskClass);
                intent.putExtra("TaskStatus",1);
                //startActivity(intent);
                ((Activity)mContext).startActivityForResult(intent, Constants.REQUEST_CODE_PROCESSING_ORDER_TASK_DETAIL);
            }
        });
        listView.getRefreshableView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,final int position, long id) {
                //if(is班组长)
                CancelTaskDialog cancleTaskDialog=new CancelTaskDialog(mContext);
                cancleTaskDialog.setTaskCancelListener(new TaskCancelListener() {
                    @Override
                    public void submitCancel(String CancelReason) {
                        datas.get(position-1);
                    }
                });
                cancleTaskDialog.show();
                return true;
            }
        });
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
    private void getProcessingDataFromServer(){
        if(RecCount!=0){
            if((pageIndex-1)*PAGE_SIZE>=RecCount){
                ToastUtil.showToastLong(R.string.noMoreData,mContext);
                return;
            }}
        showCustomDialog(R.string.loadingData);
        HttpParams params=new HttpParams();
       // params.put("id", SharedPreferenceManager.getUserName(mContext));
        //params.putHeaders("cookies",SharedPreferenceManager.getCookie(this));
       // Log.e("returnString","dd");
      //  String s=SharedPreferenceManager.getLoginData(mContext);
        //params.put("Operator_id",);
      //  JsonObjectElement jsonObjectElement=new JsonObjectElement(s);
       // String operator_id=jsonObjectElement.get("Operator_ID").valueAsString();
      //  params.put("operator_id",operator_id);
        params.put("status",1);//状态1，即处理中任务
        params.put("taskClass",TaskClass);
        params.put("pageSize",PAGE_SIZE);
        params.put("pageIndex",pageIndex);
        HttpUtils.get(mContext, "TaskList", params, new HttpCallback() {
            @Override
            public void onSuccess(String t) {
                super.onSuccess(t);
                if(t!=null) {
                    JsonObjectElement jsonObjectElement = new JsonObjectElement(t);
                   // int RecCount=jsonObjectElement.get("RecCount").valueAsInt();
                  //  if(jsonObjectElement.get("PageData")!=null&&jsonObjectElement.get("PageData").asArrayElement().size()==0){
                      //提示没有处理中的任务
                  //  }
                    if(jsonObjectElement!=null&&jsonObjectElement.get("PageData")!=null
                            &&jsonObjectElement.get("PageData").asArrayElement().size()>0) {
                        RecCount = jsonObjectElement.get("RecCount").valueAsInt();
                        if(taskNumInteface!=null){
                        taskNumInteface.ChangeTaskNumListener(0,RecCount);}
                        if (pageIndex == 1) {
                            datas.clear();
                        }
                        pageIndex++;
                        for (int i = 0; i < jsonObjectElement.get("PageData").asArrayElement().size(); i++) {
                            datas.add(jsonObjectElement.get("PageData").asArrayElement().get(i).asObjectElement());
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                taskAdapter.setDatas(datas);
                                taskAdapter.notifyDataSetChanged();
                            }
                        });
                        //      setData(datas);
                    }

                }
                dismissCustomDialog();
            }
            @Override
            public void onFailure(int errorNo, String strMsg) {

                super.onFailure(errorNo, strMsg);
             Toast toast=Toast.makeText(mContext,"获取任务列表失败，请检查网络",Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER,0,0);
                toast.show();
                dismissCustomDialog();
            }
        });
    }
    public static ProcessingFragment newInstance(String TaskClass){
        ProcessingFragment fragment = new ProcessingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Task.TASK_CLASS, TaskClass);
        fragment.setArguments(bundle);
        return fragment;
    }
    public void doRefresh(){
        pageIndex=1;
        getProcessingDataFromServer();
    }

    public void setTaskNumInteface(TaskNumInteface taskNumInteface) {
        this.taskNumInteface = taskNumInteface;
    }

    private TaskNumInteface taskNumInteface;

}
