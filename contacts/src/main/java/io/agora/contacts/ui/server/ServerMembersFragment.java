package io.agora.contacts.ui.server;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.android.arouter.launcher.ARouter;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.hyphenate.chat.EMCircleUserRole;
import com.hyphenate.chat.EMPresence;
import com.hyphenate.easeui.constants.EaseConstant;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.List;

import io.agora.common.dialog.AlertDialog;
import io.agora.contacts.R;
import io.agora.contacts.adapter.ContactListAdapter;
import io.agora.contacts.databinding.DialogUserinfoBottomBinding;
import io.agora.contacts.ui.ContactListFragment;
import io.agora.service.bean.server.ServerMemberNotifyBean;
import io.agora.service.bean.server.ServerRoleChangeNotifyBean;
import io.agora.service.callbacks.OnResourceParseCallback;
import io.agora.service.db.entity.CircleServer;
import io.agora.service.db.entity.CircleUser;
import io.agora.service.global.Constants;
import io.agora.service.managers.AppUserInfoManager;
import io.agora.service.model.ServerViewModel;
import io.agora.service.utils.EasePresenceUtil;

public class ServerMembersFragment extends ContactListFragment implements  View.OnClickListener {
    private ServerViewModel mServerViewModel;
    private CircleServer server;
    private CircleUser currentUser;
    private AlertDialog dialog;
    private CircleUser selectedUser;
    private EMCircleUserRole selfRole = EMCircleUserRole.USER;


    @Override
    protected void initConfig() {
        super.initConfig();
        mContactsListViewModel.getContactObservable().removeObservers(getViewLifecycleOwner());

        mServerViewModel = new ViewModelProvider(this).get(ServerViewModel.class);

        mServerViewModel.serverMembersLiveData.observe(this, response -> {
            finishRefresh();
            parseResource(response, new OnResourceParseCallback<List<CircleUser>>() {
                @Override
                public void onSuccess(@Nullable List<CircleUser> data) {
                    mData.clear();
                    if (data != null) {
                        mData.addAll(data);
                        setData(mData);
                    }
                }

                @Override
                public void onError(int code, String message) {
                    super.onError(code, message);
                    if (!TextUtils.isEmpty(message)) {
                        ToastUtils.showShort(message);
                    }
                }
            });
        });


        mServerViewModel.removeUserFromServerLiveData.observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<String>() {
                @Override
                public void onSuccess(@Nullable String userRemoved) {
                    ToastUtils.showShort(getString(io.agora.service.R.string.circle_removeUser_from_server_success));
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    //刷新列表
                    if (mData != null) {
                        for (int i = 0; i < mData.size(); i++) {
                            if (mData.get(i).getUsername().equals(userRemoved)) {
                                mData.remove(i);
                                i--;
                            }
                        }
                        mListAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(int code, String message) {
                    super.onError(code, message);
                    if (!TextUtils.isEmpty(message)) {
                        ToastUtils.showShort(message);
                    }
                }
            });
        });
        mServerViewModel.addModeratorLiveData.observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean data) {
                    ToastUtils.showShort(getString(io.agora.service.R.string.circle_addModerator_success));
                    selectedUser.roleID = EMCircleUserRole.MODERATOR.getRoleId();
                    refreshDialogView();
                    mListAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError(int code, String message) {
                    super.onError(code, message);
                    if (!TextUtils.isEmpty(message)) {
                        ToastUtils.showShort(message);
                    }
                }
            });
        });
        mServerViewModel.removeModeratorLiveData.observe(this, response -> {
            parseResource(response, new OnResourceParseCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean data) {
                    ToastUtils.showShort(getString(io.agora.service.R.string.circle_removeModerator_success));
                    selectedUser.roleID = EMCircleUserRole.USER.getRoleId();
                    refreshDialogView();
                    mListAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError(int code, String message) {
                    super.onError(code, message);
                    if (!TextUtils.isEmpty(message)) {
                        ToastUtils.showShort(message);
                    }
                }
            });
        });

        LiveEventBus.get(Constants.SERVER_MEMBER_LEFT_NOTIFY, ServerMemberNotifyBean.class).observe(getViewLifecycleOwner(), bean -> {
            if (bean != null && TextUtils.equals(bean.getServerId(), server.serverId)) {
                mServerViewModel.getServerMembers(server.serverId);
            }
        });
        LiveEventBus.get(Constants.SERVER_MEMBER_JOINED_NOTIFY, ServerMemberNotifyBean.class).observe(getViewLifecycleOwner(), bean -> {
            if (bean != null && TextUtils.equals(bean.getServerId(), server.serverId)) {
                mServerViewModel.getServerMembers(server.serverId);
            }
        });
        LiveEventBus.get(Constants.SERVER_ROLE_ASSIGNED_NOTIFY, ServerRoleChangeNotifyBean.class).observe(getViewLifecycleOwner(),bean->{
            if (bean != null && TextUtils.equals(bean.getServerId(), server.serverId)) {
                mServerViewModel.getServerMembers(server.serverId);
            }
        });
        AppUserInfoManager.getInstance().getCurrentUserLiveData().observe(getViewLifecycleOwner(), circleUser -> {
            this.currentUser = circleUser;
        });
        AppUserInfoManager.getInstance().getSelfServerRoleMapLiveData().observe(getViewLifecycleOwner(), serverRoleMap -> {
            if (serverRoleMap != null) {//在首页已经请求过了，所以这里必定有数据
                Integer roleId = serverRoleMap.get(server.serverId);
                if (roleId != null) {
                    if (roleId.intValue() == EMCircleUserRole.USER.getRoleId()) {
                        selfRole = EMCircleUserRole.USER;
                    } else if (roleId.intValue() == EMCircleUserRole.MODERATOR.getRoleId()) {
                        selfRole = EMCircleUserRole.MODERATOR;
                    } else if (roleId.intValue() == EMCircleUserRole.OWNER.getRoleId()) {
                        selfRole = EMCircleUserRole.OWNER;
                    }
                }
            }
        });

    }


    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        Bundle arguments = getArguments();
        server = (CircleServer) arguments.get(Constants.SERVER);
        mBinding.etSearch.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initData() {
        mRecyclerView.setAdapter(concatAdapter);
        ((ContactListAdapter) mListAdapter).setDisplayMode(ContactListAdapter.DisplayMode.SHOW_NONE);
        mServerViewModel.getServerMembers(server.serverId);
    }

    @Override
    public void onRefresh() {
        mServerViewModel.getServerMembers(server.serverId);
    }

    protected void checkView(String content) {
        super.checkView(content);
        mBinding.sideBarContact.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(View view, int position) {

//        List<CircleUser> datas = mListAdapter.getData();
//        CircleUser circleUser = datas.get(position - 1);
//        if (!TextUtils.equals(circleUser.getUsername(), AppUserInfoManager.getInstance().getCurrentUserName())) {
//            ARouter.getInstance().build("/chat/ChatActivity")
//                    .withString(EaseConstant.EXTRA_CONVERSATION_ID, circleUser.getUsername())
//                    .withInt(EaseConstant.EXTRA_CHAT_TYPE, Constants.CHATTYPE_SINGLE)
//                    .navigation();
//        }
    }

    @Override
    public void onItemSubViewClick(View view, int position) {
        if (view.getId() == R.id.presenceView) {
            List<CircleUser> datas = mListAdapter.getData();
            if (datas != null&&position<datas.size()&&position>=0) {
                CircleUser circleUser = datas.get(position);
                if (circleUser != null) {
                    if (!TextUtils.equals(circleUser.username, AppUserInfoManager.getInstance().getCurrentUserName())) {
                        showUserInfoBottomDialog(circleUser);
                    }
                }
            }
        }
    }

    private void showUserInfoBottomDialog(CircleUser user) {
        if (user != null) {
            //根据user初始化selectedMemberRole、isSelectedMemberMuteState
            this.selectedUser = user;
            DialogUserinfoBottomBinding dialogBinding = DialogUserinfoBottomBinding.inflate(getLayoutInflater());
            dialog = new AlertDialog.Builder(mContext)
                    .setContentView(dialogBinding.getRoot())
                    .setOnClickListener(R.id.tv_chat, this)
                    .setOnClickListener(R.id.tv_mute, this)
                    .setOnClickListener(R.id.tv_set_role, this)
                    .setOnClickListener(R.id.tv_kick, this)
                    .setText(R.id.tv_nick_name, TextUtils.isEmpty(user.getNickname()) ? user.getUsername() : user.getNickname())
                    .setText(R.id.tv_id, getString(io.agora.service.R.string.hx_id) + user.getUsername())
                    .setGravity(Gravity.BOTTOM)
                    .setLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .show();
            dialog.getViewById(R.id.tv_mute).setVisibility(View.GONE);
            Glide.with(mContext)
                    .load(user.getAvatar())
                    .placeholder(io.agora.service.R.drawable.circle_default_avatar)
                    .into(dialogBinding.ivUserAvatar);
            EMPresence presence = AppUserInfoManager.getInstance().getPresences().get(user.getUsername());
            int presenceIcon = EasePresenceUtil.getPresenceIcon(mContext, presence);
            dialogBinding.ivPresence.setImageResource(presenceIcon);
            refreshDialogView();
        }
    }

    private void refreshDialogView() {
        if (dialog != null) {
            TextView tvChat = dialog.getViewById(R.id.tv_mute);
            TextView tvMute = dialog.getViewById(R.id.tv_mute);
            TextView tvSetAdmin = dialog.getViewById(R.id.tv_set_role);
            TextView tvKick = dialog.getViewById(R.id.tv_kick);
            ImageView ivMute = dialog.getViewById(R.id.iv_mute);
            TextView tvRoleTag = dialog.getViewById(R.id.tv_role_tag);
            tvChat.setVisibility(View.VISIBLE);
            tvMute.setVisibility(View.GONE);
            tvSetAdmin.setVisibility(View.VISIBLE);
            tvKick.setVisibility(View.VISIBLE);
            ivMute.setVisibility(View.VISIBLE);
            tvRoleTag.setVisibility(View.VISIBLE);

            if (selfRole == EMCircleUserRole.MODERATOR) {
                tvSetAdmin.setVisibility(View.GONE);
            } else if (selfRole == EMCircleUserRole.USER) {
                tvSetAdmin.setVisibility(View.GONE);
                tvMute.setVisibility(View.GONE);
                tvKick.setVisibility(View.GONE);
            }
            tvKick.setText(getString(io.agora.service.R.string.dialog_kick_server));
            Drawable drawable = null;
            if (selectedUser.isMuted) {
                ivMute.setVisibility(View.VISIBLE);
                tvMute.setText(getString(io.agora.service.R.string.dialog_member_unmute));
                drawable = getResources().getDrawable(io.agora.service.R.drawable.circle_member_unmute);
            } else {
                ivMute.setVisibility(View.GONE);
                tvMute.setText(getString(io.agora.service.R.string.dialog_member_mute));
                drawable = getResources().getDrawable(io.agora.service.R.drawable.circle_member_mute);
            }
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            tvMute.setCompoundDrawables(null, drawable, null, null);

            if (selectedUser.roleID == EMCircleUserRole.USER.getRoleId()) {
                tvSetAdmin.setText(getString(io.agora.service.R.string.dialog_set_admin));
                drawable = getResources().getDrawable(io.agora.service.R.drawable.circle_make_admin);
                tvRoleTag.setVisibility(View.GONE);
            } else if (selectedUser.roleID == EMCircleUserRole.MODERATOR.getRoleId()) {
                tvSetAdmin.setText(getString(io.agora.service.R.string.dialog_cancel_admin));
                drawable = getResources().getDrawable(io.agora.service.R.drawable.circle_cancel_admin);
                tvRoleTag.setVisibility(View.VISIBLE);
                tvRoleTag.setEnabled(true);
                tvRoleTag.setText(getString(R.string.circle_role_moderator));
            }
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            tvSetAdmin.setCompoundDrawables(null, drawable, null, null);

            //当目标是群主时
            if (selectedUser.roleID == EMCircleUserRole.OWNER.getRoleId()) {
                tvRoleTag.setVisibility(View.VISIBLE);
                tvRoleTag.setEnabled(false);
                tvRoleTag.setText(getString(R.string.circle_role_creater));

                tvMute.setVisibility(View.GONE);
                tvSetAdmin.setVisibility(View.GONE);
                tvKick.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(View v) {
         if (v.getId() == R.id.tv_chat) {
            //发起私聊
            if (selectedUser != null) {
                ARouter.getInstance().build("/chat/ChatActivity")
                        .withString(EaseConstant.EXTRA_CONVERSATION_ID, selectedUser.getUsername())
                        .withInt(EaseConstant.EXTRA_CHAT_TYPE, Constants.CHATTYPE_SINGLE)
                        .navigation();
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        } else if (v.getId() == R.id.tv_set_role) {
            //设为/移除管理员
            if (selectedUser != null) {
                if (selectedUser.roleID == EMCircleUserRole.USER.getRoleId()) {
                    mServerViewModel.addModeratorToServer(server.serverId, selectedUser.getUsername());
                } else if (selectedUser.roleID == EMCircleUserRole.MODERATOR.getRoleId()) {
                    mServerViewModel.removeModeratorFromServer(server.serverId, selectedUser.getUsername());
                }
            }
        } else if (v.getId() == R.id.tv_kick) {
            //踢出社区
            if (selectedUser != null) {
                mServerViewModel.removeUserFromServer(server.serverId, selectedUser.getUsername());
            }
        }
    }
}
