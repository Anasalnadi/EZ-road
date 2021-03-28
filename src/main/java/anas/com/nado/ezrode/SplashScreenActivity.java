package anas.com.nado.ezrode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.internal.service.Common;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import anas.com.nado.ezrode.Model.DriverInfoModel;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;

public class SplashScreenActivity extends AppCompatActivity {
    private final static int REQUES_CODE_LOGIN=333;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener ;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    FirebaseDatabase database;
    DatabaseReference driverdatabaseReference; // driverInfoRef


    @Override
    protected void onStart() {
        super.onStart();
        splahScreenDelay();
    }

    @Override
    protected void onStop() {
        if(firebaseAuth != null && listener != null){
            firebaseAuth.removeAuthStateListener(listener);
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        init();
        // check permission
        Dexter.withContext(getApplicationContext()).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        //Error ??
                        Toast.makeText(getApplicationContext(), "nice"
                                , Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getApplicationContext(), "permission"+permissionDeniedResponse.getPermissionName()+""+" was denied"
                                , Toast.LENGTH_SHORT).show();

                        //finish();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();

    }

    private void init() {

        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        driverdatabaseReference = database.getReference(Comon.DRIVER_INFO_REFERENC);

        providers= Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build()
                ,new AuthUI.IdpConfig.GoogleBuilder().build());

        firebaseAuth=FirebaseAuth.getInstance();
        listener = myFirebaseAuth ->{
            FirebaseUser user =myFirebaseAuth.getCurrentUser();
            if(user != null){// check if register or not
                checkUserFromFireBass();
            }else {
                showLoginLayout();
            }
        };
    }

    private void checkUserFromFireBass() {
        driverdatabaseReference.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).
                addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            //Toast.makeText(SplashScreenActivity.this,"User already register",Toast.LENGTH_SHORT).show();
                            DriverInfoModel driverInfoModel=snapshot.getValue(DriverInfoModel.class);
                            goToHomeActivity(driverInfoModel);
                        }else{
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void goToHomeActivity(DriverInfoModel driverInfoModel) {
        Comon.currentUser=driverInfoModel;
        startActivity(new Intent(SplashScreenActivity.this,DriverHomeActivity.class));
        finish();
    }

    private void showRegisterLayout(){
        AlertDialog.Builder builder= new AlertDialog.Builder(this,R.style.DialogTheme);
        View iteamView= LayoutInflater.from(this).inflate(R.layout.layout_register,null);

        TextInputEditText edit_first_name= iteamView.findViewById(R.id.edit_first_name);
        TextInputEditText edit_last_name= iteamView.findViewById(R.id.edit_last_name);
        TextInputEditText edit_phone=iteamView.findViewById(R.id.edit_phone_number);
        Button button_continue=iteamView.findViewById(R.id.button_register);

        //set data
        if(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() !=null &&
               !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
            edit_phone.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        //set View
        builder.setView(iteamView);
        AlertDialog dialog=builder.create();
        dialog.show();// problem 8)!!

        button_continue.setOnClickListener(view -> {
            if (TextUtils.isEmpty(edit_first_name.getText().toString()))
            {
                Toast.makeText(this,"Please enter first name",Toast.LENGTH_SHORT).show();
                return;
            }else if (TextUtils.isEmpty(edit_last_name.getText().toString()))
            {
                Toast.makeText(this,"Please enter last name",Toast.LENGTH_SHORT).show();

            }else if (TextUtils.isEmpty(edit_phone.getText().toString()))
            {
                Toast.makeText(this,"Please enter phone Number",Toast.LENGTH_SHORT).show();
            }else {

                DriverInfoModel driverInfoModel =new DriverInfoModel();
                driverInfoModel.setFirstName(edit_first_name.getText().toString());
                driverInfoModel.setLastName((edit_last_name.getText().toString()));
                driverInfoModel.setPhoneNumber(edit_phone.getText().toString());
                driverInfoModel.setRating(0.0);

                driverdatabaseReference.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(driverInfoModel)
                        .addOnFailureListener(e ->
                                {
                                    dialog.dismiss();
                                    Toast.makeText(SplashScreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        )
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Register Succesfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            goToHomeActivity(driverInfoModel);
                        });
            }

        });
    }

    private void showLoginLayout(){
        AuthMethodPickerLayout authMethodPickerLayout =new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phon_signIn)
                .setGoogleButtonId(R.id.btn_google_signIn)
                .build();
        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder().setAuthMethodPickerLayout(authMethodPickerLayout)
            .setIsSmartLockEnabled(false).setTheme(R.style.LoginTheem)
            .setAvailableProviders(providers)
            .build(),REQUES_CODE_LOGIN);
    }

    private void splahScreenDelay() {

        progressBar.setVisibility(View.VISIBLE);

        Completable.timer(3, TimeUnit.SECONDS
                , AndroidSchedulers.mainThread()).subscribe(() ->

                //after show splash Screen ,ask login if not Login
                firebaseAuth.addAuthStateListener(listener)

                );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUES_CODE_LOGIN){
            IdpResponse response=IdpResponse.fromResultIntent(data);
            if(resultCode == RESULT_OK){

                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
            }else {
                Toast.makeText(this,"(Errors): "+response.getError().getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }
}