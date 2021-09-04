package cn.pivotstudio.husthole.view.homescreen.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import cn.pivotstudio.husthole.R;
import cn.pivotstudio.husthole.model.CheckingToken;
import cn.pivotstudio.husthole.network.RequestInterface;
import cn.pivotstudio.husthole.network.RetrofitManager;
import cn.pivotstudio.husthole.view.homescreen.mine.AboutActivity;
import cn.pivotstudio.husthole.view.homescreen.mine.HoleStarReplyActivity;
import cn.pivotstudio.husthole.view.homescreen.mine.RulesActivity;
import cn.pivotstudio.husthole.view.homescreen.mine.ScreenActivity;
import cn.pivotstudio.husthole.view.homescreen.mine.SettingsActivity;
import cn.pivotstudio.husthole.view.homescreen.mine.ShareCardActivity;
import cn.pivotstudio.husthole.view.homescreen.mine.UpdateActivity;
import cn.pivotstudio.husthole.view.homescreen.publishhole.PublishHoleActivity;
import cn.pivotstudio.husthole.view.registerandlogin.activity.WelcomeActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class MineFragment extends Fragment {

    private View rootView,shareCardView,backgroundView;
    private RelativeLayout settings,shield, rules, share, evaluate, advice, about, update, logout;
    private LinearLayout myHole, myStar, myReply, shareCard;
    private TextView tv_joinDays,tv_myStarNum,tv_myHoleNum,tv_myReplyNum,cancel,location;
    private static String BASE_URL = "http://husthole.pivotstudio.cn/api/";
    private org.json.JSONArray jsonArray;
    private PopupWindow ppwBackground, ppwShare;
    
    Retrofit retrofit;
    RequestInterface request;
    String TAG = "isMine";

    public static MineFragment newInstance() {
        MineFragment fragment = new MineFragment();
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.page4fragment, container, false);
        settings = rootView.findViewById(R.id.settings);
        shield=rootView.findViewById(R.id.shield);
        rules = rootView.findViewById(R.id.rules);
        share = rootView.findViewById(R.id.share);
        evaluate = rootView.findViewById(R.id.evaluate);
        advice = rootView.findViewById(R.id.advice);
        about = rootView.findViewById(R.id.about);
        update = rootView.findViewById(R.id.update);
        logout = rootView.findViewById(R.id.logout);

        myHole = rootView.findViewById(R.id.my_hole);
        myStar = rootView.findViewById(R.id.my_star);
        myReply = rootView.findViewById(R.id.my_reply);

        tv_joinDays = rootView.findViewById(R.id.my_date);
        tv_myHoleNum = rootView.findViewById(R.id.my_hole_num);
        tv_myStarNum = rootView.findViewById(R.id.my_star_num);
        tv_myReplyNum = rootView.findViewById(R.id.my_reply_num);

        location = rootView.findViewById(R.id.ppw_share_location);

        shareCardView = LayoutInflater.from(getContext()).inflate(R.layout.ppw_share, null);
        backgroundView = LayoutInflater.from(getContext()).inflate(R.layout.ppw_share_card_darkscreen, null);
        shareCard = shareCardView.findViewById(R.id.share_card);
        cancel = shareCardView.findViewById(R.id.share_cancel_button);

        ppwShare=new PopupWindow(shareCardView);
        ppwShare.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        ppwShare.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        ppwShare.setAnimationStyle(R.style.Page2Anim);

        ppwBackground=new PopupWindow(backgroundView);
        ppwBackground.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        ppwBackground.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        ppwBackground.setAnimationStyle(R.style.darkScreenAnim);

        RetrofitManager.RetrofitBuilder(BASE_URL);
        retrofit = RetrofitManager.getRetrofit();
        request = retrofit.create(RequestInterface.class);
        getMyData();

        settings.setOnClickListener(this::onClick);
        shield.setOnClickListener(this::onClick);
        rules.setOnClickListener(this::onClick);
        share.setOnClickListener(this::onClick);
        evaluate.setOnClickListener(this::onClick);
        advice.setOnClickListener(this::onClick);
        about.setOnClickListener(this::onClick);
        update.setOnClickListener(this::onClick);
        logout.setOnClickListener(this::onClick);
        myHole.setOnClickListener(this::onClick);
        myStar.setOnClickListener(this::onClick);
        myReply.setOnClickListener(this::onClick);

        backgroundView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ppwShare.dismiss();
                ppwBackground.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ppwShare.dismiss();
                ppwBackground.dismiss();
            }
        });
        shareCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getApplicationContext(), ShareCardActivity.class);
                startActivity(intent);
            }
        });

        getMyData();

        return rootView;
    }

    private void getMyData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Call<ResponseBody> call = request.myData();//进行封装
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.d(TAG,"in onResponse");
                        try {
                            if(response.errorBody() != null){
                                Log.d(TAG,"in error: " + response.errorBody());
                                Log.d(TAG,"in error: " + response.errorBody().string());
                            }
                            if (response.body() != null) {
                                String jsonStr = response.body().string();
                                Log.d(TAG,"1: " + response.body());
                                Log.d(TAG,"2: " + jsonStr + "--");
//                                jsonArray=new org.json.JSONArray(jsonStr);
//                                JSONObject data = jsonArray.getJSONObject(0);
                                JSONObject data = new JSONObject(jsonStr);
                                int joinDays = data.getInt("join_days");
                                int myHoleNum = data.getInt("hole_sum");
                                int myStarNum = data.getInt("follow_num");
                                int myReplyNum = data.getInt("replies_num");
                                Log.d(TAG,joinDays+"");
                                String days = "我来到树洞已经<font color=\"#02A9F5\">"+joinDays+"</font>天啦。";
                                if(CheckingToken.IfTokenExist()){
                                    Log.d(TAG,"已登陆");
                                    tv_joinDays.setText(Html.fromHtml(days));
                                    tv_myHoleNum.setText( String.valueOf(myHoleNum));
                                    tv_myStarNum.setText( String.valueOf(myStarNum));
                                    tv_myReplyNum.setText(String.valueOf(myReplyNum));
                                }else {
                                    days = "我来到树洞已经<font color=\"#02A9F5\">"+0+"</font>天啦。";
                                    tv_joinDays.setText(Html.fromHtml(days));
                                    tv_myHoleNum.setText( String.valueOf(0));
                                    tv_myStarNum.setText( String.valueOf(0));
                                    tv_myReplyNum.setText(String.valueOf(0));
                                }
                            }else{
                                Log.d(TAG,"is null");
                            }
                        }catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) { }
                });
            }
        }).start();
    }

    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.my_hole:
                intent = new Intent(getActivity().getApplicationContext(), HoleStarReplyActivity.class);
                intent.putExtra("initFragmentId",0);
                startActivity(intent);
                break;
            case R.id.my_star:
                intent = new Intent(getActivity().getApplicationContext(), HoleStarReplyActivity.class);
                intent.putExtra("initFragmentID",1);
                startActivity(intent);
                break;
            case R.id.my_reply:
                intent = new Intent(getActivity().getApplicationContext(), HoleStarReplyActivity.class);
                intent.putExtra("initFragmentID",2);
                startActivity(intent);
                break;
            case R.id.settings:
                intent = new Intent(getActivity().getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.shield:
                intent = new Intent(getActivity().getApplicationContext(), ScreenActivity.class);
                startActivity(intent);
                break;
            case R.id.rules:
                intent = new Intent(getActivity().getApplicationContext(), RulesActivity.class);
                startActivity(intent);
                break;
            case R.id.share:
                ppwBackground.showAsDropDown(rootView);
                ppwShare.showAsDropDown(location);
                break;
            case R.id.about:
                intent = new Intent(getActivity().getApplicationContext(), AboutActivity.class);
                startActivity(intent);
                break;
            case R.id.update:
                intent = new Intent(getActivity().getApplicationContext(), UpdateActivity.class);
                startActivity(intent);
                break;
            case R.id.logout:
                Dialog dialog = new Dialog(getContext());
                View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_logout, null);
                dialog.setContentView(dialogView);
                Button btn_cancel = dialogView.findViewById(R.id.cancel);
                Button btn_logout = dialogView.findViewById(R.id.logout);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                btn_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                btn_logout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        dialog.dismiss();
                        SharedPreferences.Editor editor = getContext().getSharedPreferences("Depository", Context.MODE_PRIVATE).edit();//获取编辑器
                        editor.putString("token", "");
                        editor.commit();
                        Intent intent=new Intent(getContext(), WelcomeActivity.class);
                        startActivity(intent);
                    }
                });
                dialog.show();
                break;
        }
    }
}
