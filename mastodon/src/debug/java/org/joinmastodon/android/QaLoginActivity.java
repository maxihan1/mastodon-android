// 디버그 빌드에만 있는 테스트 입구 — 서버 주소와 토큰을 받아 그 계정으로 로그인된 채 홈을 연다(QA 자동화가 로그인 화면을 건너뛸 때 쓴다)
package org.joinmastodon.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Token;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

/**
 * 실행 값(intent extras): server(서버 주소) · token(접근 토큰) · clientId · clientSecret(토큰을 만든 OAuth 앱 — 로그아웃 때 토큰 폐기에 쓴다).
 * 값이 없거나 로그인이 실패하면 로그인 없이 앱을 연다 — 테스트는 그때 화면으로 로그인한다.
 * src/debug 에만 있어 배포 빌드(release · beta · githubRelease)에는 이 화면도 매니페스트 항목도 들어가지 않는다.
 */
public class QaLoginActivity extends Activity{
	private static final String TAG="QaLoginActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Intent in=getIntent();
		String domain=in.getStringExtra("server");
		String accessToken=in.getStringExtra("token");
		if(TextUtils.isEmpty(domain) || TextUtils.isEmpty(accessToken)){
			Log.w(TAG, "server · token 이 없어 로그인 없이 연다");
			openMain();
			return;
		}
		Token token=new Token();
		token.accessToken=accessToken;
		token.tokenType="Bearer";
		token.scope=AccountSessionManager.SCOPE;
		token.createdAt=System.currentTimeMillis()/1000;
		Application app=new Application();
		app.name="QA login inject";
		app.clientId=in.getStringExtra("clientId");
		app.clientSecret=in.getStringExtra("clientSecret");
		AccountSessionManager.loadInstanceInfo(domain, new Callback<>(){
			@Override
			public void onSuccess(Instance instance){
				new GetOwnAccount(true)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Account account){
								AccountSessionManager.getInstance().addAccount(instance, token, account, app, null);
								Log.i(TAG, "로그인된 채로 연다: @"+account.acct);
								openMain();
							}

							@Override
							public void onError(ErrorResponse error){
								fail("계정", error);
							}
						})
						.exec(domain, token);
			}

			@Override
			public void onError(ErrorResponse error){
				fail("서버 정보", error);
			}
		});
	}

	private void fail(String what, ErrorResponse error){
		Log.e(TAG, what+"를 못 읽어 로그인 없이 연다: "+error);
		openMain();
	}

	private void openMain(){
		Intent intent=new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}
}
