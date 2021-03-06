package com.emms.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.datastore_android_sdk.datastore.ObjectElement;
import com.datastore_android_sdk.rest.JsonObjectElement;
import com.emms.R;
import com.emms.activity.CreateTaskActivity;
import com.emms.activity.InvitorActivity;
import com.emms.activity.SubTaskManageActivity;
import com.emms.activity.SummaryActivity;
import com.emms.activity.TaskCompleteActivity;
import com.emms.activity.WorkLoadActivity;
import com.emms.schema.Data;
import com.emms.schema.Task;
import com.emms.util.BaseData;
import com.emms.util.Constants;
import com.emms.util.DataUtil;
import com.emms.util.RootUtil;
import com.emms.util.ToastUtil;
import com.zxing.android.CaptureActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public abstract class PopMenuTaskDetail {
	private ArrayList<String> itemList;
	private Context context;
	private PopupWindow popupWindow;
	private PopMenuTaskDetail popMenuTaskDetail=this;
	private ListView listView;
	private PopAdapter popAdapter;
    private ObjectElement TaskDetail;
	private Long TaskId;
	private String TaskClass;


	public void setHasNFC(boolean hasNFC) {
		isHasNFC = hasNFC;
	}

	private boolean isHasNFC=false;

	public NFCDialog getNfcDialog() {
		return nfcDialog;
	}

	public void setNfcDialog(NFCDialog nfcDialog) {
		this.nfcDialog = nfcDialog;
	}

	private NFCDialog nfcDialog;
	public void setIs_Main_person_in_charge_Operator_id(boolean is_Main_person_in_charge_Operator_id) {
		this.is_Main_person_in_charge_Operator_id = is_Main_person_in_charge_Operator_id;
	}
    private boolean taskComplete=false;
	private boolean is_Main_person_in_charge_Operator_id=false;

	public void setHasEquipment(boolean hasEquipment) {
		this.hasEquipment = hasEquipment;
	}

	private boolean hasEquipment=false;

	public void setEquipmentNum(int equipmentNum) {
		EquipmentNum = equipmentNum;
	}

	private int EquipmentNum=0;
	private HashMap<String,Integer> item_image_mapping=new HashMap<>();
	// private OnItemClickListener listener;
	public PopMenuTaskDetail(Context context, int width,String taskDetail,String taskClass) {
		// TODO Auto-generated constructor stub

		this.context = context;
		item_image_mapping.put(context.getResources().getString(R.string.menu_list_create_task),R.mipmap.create);
		item_image_mapping.put(context.getResources().getString(R.string.sacn_qr_code),R.mipmap.more_scan);
		item_image_mapping.put(context.getResources().getString(R.string.menu_list_fault_summary),R.mipmap.failure_summary);
		item_image_mapping.put(context.getResources().getString(R.string.menu_list_workload_input),R.mipmap.more_input);
		item_image_mapping.put(context.getResources().getString(R.string.menu_list_sub_task_manage),R.mipmap.sub_task_management);
		item_image_mapping.put(context.getResources().getString(R.string.menu_list_invite_help),R.mipmap.more_invitation);
		item_image_mapping.put(context.getResources().getString(R.string.menu_list_transfer_order),R.mipmap.more_single_turn);
		item_image_mapping.put(context.getResources().getString(R.string.menu_list_task_complete),R.mipmap.more_finish);
        item_image_mapping.put(context.getResources().getString(R.string.Refresh),R.mipmap.refresh);
		this.TaskDetail=new JsonObjectElement(taskDetail);
		TaskId=TaskDetail.get(Task.TASK_ID).valueAsLong();
		TaskClass=taskClass;
		itemList = new ArrayList<>(5);
		View view = LayoutInflater.from(context)
				.inflate(R.layout.popmenu, null);
		final RelativeLayout layout=(RelativeLayout)view.findViewById(R.id.popup_view_cont);
		layout.setBackgroundColor(Color.argb(80,0,0,0));
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		popAdapter =new PopAdapter();
		// 设置 listview
		listView = (ListView) view.findViewById(R.id.listView);
		//listView.setPaddingRelative();
		listView.setBackgroundColor(Color.WHITE);
		listView.setAdapter(popAdapter);
		listView.setFocusableInTouchMode(true);
		listView.setFocusable(true);
		setOnItemClickListener();
      //  ListViewUtility.setListViewHeightBasedOnChildren(listView);
//		popupWindow = new PopupWindow(view, 254, LayoutParams.WRAP_CONTENT);

		popupWindow = new PopupWindow(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		// 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景（很神奇的）
		popupWindow.setBackgroundDrawable(new BitmapDrawable());

		popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				onEventDismiss();
			}
		});
	}

	public abstract void onEventDismiss();
	public void refreshData(){
		popAdapter.notifyDataSetChanged();
	}
	// 设置菜单项点击监听器
	public void setOnItemClickListener() {
		// this.listener = listener;
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(itemList.get(position).equals(context.getString(R.string.menu_list_create_task))){
				   CreateTask();
				}else if(itemList.get(position).equals(context.getString(R.string.sacn_qr_code))){
					ScanQRCode();
				} else if (itemList.get(position).equals(context.getString(R.string.menu_list_fault_summary))) {
					FaultSummary();
				}else if (itemList.get(position).equals(context.getString(R.string.menu_list_workload_input))) {
					WorkloadInput();
				}else if(itemList.get(position).equals(context.getString(R.string.menu_list_sub_task_manage))){
					SubTaskManage();
				}else if(itemList.get(position).equals(context.getString(R.string.menu_list_invite_help))){
					InviteHelp();
				}else if(itemList.get(position).equals(context.getString(R.string.menu_list_transfer_order))){
					ExChangeOrder();
				}else if(itemList.get(position).equals(context.getString(R.string.menu_list_task_complete))){
					TaskComplete();
				}else if (itemList.get(position).equals(context.getString(R.string.Refresh))){
					Refresh();
				}
				popMenuTaskDetail.dismiss();
			}
		});

	}

	// 批量添加菜单项
	public void addItems(String[] items) {
		itemList.clear();
		Collections.addAll(itemList, items);
		switch (DataUtil.isDataElementNull(BaseData.getConfigData().get(BaseData.TASK_COMPLETE_SHOW_WORKLOAD_ACTION))){
			case "1":{
				if(itemList.contains(context.getString(R.string.menu_list_workload_input))){
					itemList.remove(context.getString(R.string.menu_list_workload_input));
				}
				break;
			}
			default:{
				//do nothing
				break;
			}
		}
	}

	// 批量添加菜单项
	public void addItems(ArrayList items) {
		this.itemList =items;
	}

	// 单个添加菜单项
	public void addItem(String item) {
		itemList.add(item);
	}

	// 下拉式 弹出 pop菜单 parent 右下角
	public void showAsDropDown(View parent) {
		popupWindow.showAsDropDown(parent,
				10,
				// 保证尺寸是根据屏幕像素密度来的
				-1);

		// 使其聚集
		popupWindow.setFocusable(true);
		// 设置允许在外点击消失
		popupWindow.setOutsideTouchable(true);
		// 刷新状态
		popupWindow.update();
	}


	public boolean isShowing(){

		return 	popupWindow.isShowing();
	}
	// 隐藏菜单
	public void dismiss() {
		popupWindow.update();
		popupWindow.dismiss();
	}

	// 适配器
	private final class PopAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return itemList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return itemList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHolder holder;
			if (convertView == null) {
				convertView = LayoutInflater.from(context).inflate(
						R.layout.popuwindow_task, null);
				holder = new ViewHolder();

				convertView.setTag(holder);

				holder.groupItem = (TextView) convertView
						.findViewById(R.id.textView);
				holder.imageView = (ImageView) convertView.findViewById(R.id.menu_detail);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.groupItem.setText(itemList.get(position));
			Drawable img  = context.getResources().getDrawable(R.mipmap.more_input);
//			if (0 ==position) {
//				img = context.getResources().getDrawable(R.mipmap.create);
//			}else if (1 ==position){
//				img =context.getResources().getDrawable(R.mipmap.failure_summary);
//			}else if (2 == position){
//				img =context.getResources().getDrawable(R.mipmap.more_input);
//			}else if (3 == position){
//				img =context.getResources().getDrawable(R.mipmap.sub_task_management);
//			}else if (4 == position){
//				img =context.getResources().getDrawable(R.mipmap.more_invitation);
//			}else if (5 == position){
//				img =context.getResources().getDrawable(R.mipmap.more_single_turn);
//			}else if (6 == position){
//				img =context.getResources().getDrawable(R.mipmap.more_finish);
//			}
			img=context.getResources().getDrawable(item_image_mapping.get(itemList.get(position)));
			// 调用setCompoundDrawables时，必须调用Drawable.setBounds()方法,否则图片不显示
//			img.setBounds(0, 0, img.getMinimumWidth(), img.getMinimumHeight());
//
//			holder.groupItem.setCompoundDrawables(img,null,null,null);
			holder.imageView.setImageDrawable(img);
			return convertView;
		}

		private final class ViewHolder {
			TextView groupItem;
			ImageView imageView;
		}
	}
	private void WorkloadInput(){

		if(!is_Main_person_in_charge_Operator_id){
			ToastUtil.showToastShort(R.string.OnlyMainPersonCanWriteWorkload,context);
			return;
		}
		Intent intent=new Intent(context, WorkLoadActivity.class);
		//intent.putExtra(Task.TASK_ID,TaskId);
		intent.putExtra(Task.TASK_CLASS,TaskClass);
		intent.putExtra("TaskDetail",TaskDetail.toString());
		context.startActivity(intent);
	}
//	private void Scan(){
//
//	}
	private void ExChangeOrder(){
		if(!is_Main_person_in_charge_Operator_id){
			ToastUtil.showToastShort(R.string.OnlyMainPersonCanChangeOrder,context);
			return;
		}
		if(taskComplete){
			ToastUtil.showToastShort(R.string.TaskEquipmentIsCompleteCanNotChangeOrder,context);
			return;
		}
		Intent intent=new Intent(context, InvitorActivity.class);
		intent.putExtra(Task.TASK_ID,String.valueOf(TaskId));
		intent.putExtra("isExChangeOrder",true);
		((Activity)context).startActivityForResult(intent, Constants.REQUEST_CODE_EXCHANGE_ORDER);
	}
	private void InviteHelp(){
		if(!is_Main_person_in_charge_Operator_id){
			ToastUtil.showToastShort(R.string.OnlyMainPersonCanInvitePeople,context);
			return;
		}
		if(taskComplete){
			ToastUtil.showToastShort(R.string.TaskEquipmentIsCompleteCanNotInvite,context);
			return;
		}
		Intent intent=new Intent(context, InvitorActivity.class);
		intent.putExtra(Task.TASK_ID,String.valueOf(TaskId));
		intent.putExtra("isInviteHelp",true);
		context.startActivity(intent);

	}
	private void SubTaskManage(){
		Intent intent=new Intent(context, SubTaskManageActivity.class);
		//intent.putExtra(Task.TASK_ID,TaskId);
		intent.putExtra("TaskDetail",TaskDetail.toString());
		context.startActivity(intent);
	}
//	private void FailureSummary(){
//
//	}
//	private void setTaskIdFromActivity(Long taskId){
//		this.TaskId=taskId;
//	}
	private void FaultSummary(){
		if(!is_Main_person_in_charge_Operator_id){
			ToastUtil.showToastShort(R.string.OnlyMainPersonCanWriteFaultSummary,context);
			return;
		}
		if(RootUtil.rootTaskClass(TaskClass,Task.REPAIR_TASK)
				|| RootUtil.rootTaskClass(TaskClass,Task.GROUP_ARRANGEMENT) ){
		Intent intent=new Intent(context, SummaryActivity.class);
		//intent.putExtra(Task.TASK_ID,TaskId);
		intent.putExtra("TaskDetail",TaskDetail.toString());
		intent.putExtra(Task.TASK_CLASS, TaskClass);
		context.startActivity(intent);}else {
			ToastUtil.showToastShort(R.string.judgeTaskClass,context);
		}
	}
	private void TaskComplete() {
		if (!is_Main_person_in_charge_Operator_id) {
			ToastUtil.showToastShort(R.string.OnlyMainPersonCanSubmitTaskComplete, context);
			return;
		}
		if (hasEquipment && EquipmentNum <= 0) {
			ToastUtil.showToastShort(R.string.TaskHasNoEquipment, context);
			return;
		}
		if (!taskComplete) {
			if (hasEquipment) {
				ToastUtil.showToastShort(R.string.TaskEquipmentNotComplete, context);
			} else {
				ToastUtil.showToastShort(R.string.CanNotCompleteTask, context);
			}
			return;
		}
		if (TaskClass != null && TaskClass.equals(Task.MOVE_CAR_TASK)) {
//			if(isHasNFC) {
//				if (nfcDialog != null && !nfcDialog.isShowing()) {
//					nfcDialog.show();
//				}
//			}else {
//				TaskCompleteDialog taskCompleteDialog=new TaskCompleteDialog(context,R.style.MyDialog);
//				taskCompleteDialog.setTask_ID(DataUtil.isDataElementNull(TaskDetail.get(Task.TASK_ID)));
//				taskCompleteDialog.setTaskClass(TaskClass);
//				taskCompleteDialog.show();
//			}
			Intent intent=new Intent(context, TaskCompleteActivity.class);
			intent.putExtra("TaskDetail", TaskDetail.toString());
			intent.putExtra("TaskComplete", true);
			intent.putExtra(Task.TASK_CLASS, TaskClass);
			context.startActivity(intent);
		} else if(TaskClass != null && TaskClass.equals(Task.TRANSFER_MODEL_TASK)){
			Intent intent = new Intent(context, SummaryActivity.class);
			//intent.putExtra(Task.TASK_ID,TaskId);
			intent.putExtra("TaskDetail", TaskDetail.toString());
			intent.putExtra("TaskComplete", true);
			intent.putExtra(Task.TASK_CLASS, TaskClass);
			context.startActivity(intent);
		}else {
		Intent intent = new Intent(context, SubTaskManageActivity.class);
		//intent.putExtra(Task.TASK_ID,TaskId);
		intent.putExtra("TaskDetail", TaskDetail.toString());
		intent.putExtra("TaskComplete", true);
		intent.putExtra(Task.TASK_CLASS, TaskClass);
		context.startActivity(intent);
	}
	}
	private void CreateTask(){
		Intent intent=new Intent(context, CreateTaskActivity.class);
		intent.putExtra("FromTask_ID",String.valueOf(TaskId));
		context.startActivity(intent);
	}
	private void ScanQRCode(){
		if(!hasEquipment){
			ToastUtil.showToastShort(R.string.error_add_equipment,context);
			return;
		}
		((Activity)context).startActivityForResult(new Intent(context, CaptureActivity.class),Constants.REQUEST_CODE_TASK_DETAIL_TO_CAPTURE_ACTIVITY);
	}
	private void Refresh(){
		if(onTaskDetailRefreshListener!=null){
			onTaskDetailRefreshListener.onRefresh();
		}
	}
//	public void setDialogOnSubmit(dialogOnSubmitInterface dialogOnSubmit) {
//		this.dialogOnSubmit = dialogOnSubmit;
//	}
//
//	private dialogOnSubmitInterface dialogOnSubmit;
	public void setTaskComplete(boolean taskComplete){
		this.taskComplete=taskComplete;
	}

	public interface OnTaskDetailRefreshListener{
		void onRefresh();
	}

	public OnTaskDetailRefreshListener getOnTaskDetailRefreshListener() {
		return onTaskDetailRefreshListener;
	}

	public void setOnTaskDetailRefreshListener(OnTaskDetailRefreshListener onTaskDetailRefreshListener) {
		this.onTaskDetailRefreshListener = onTaskDetailRefreshListener;
	}

	private OnTaskDetailRefreshListener onTaskDetailRefreshListener;
}
