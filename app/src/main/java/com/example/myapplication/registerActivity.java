package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class registerActivity extends AppCompatActivity {
    private static final int PICK_FROM_ALBLM = 1;
    private static final int CROP_FROM_IMAGE = 2;
    private Uri imageUri;
    private String pathUri;
    private File tempFile;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private String TAG;
    private EditText editText2;
    private EditText editText3;
    private EditText editText1;
    private Long editText4;
    private EditText editText5;
    private EditText editText6;
    private ImageView profile;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();


        editText1 = findViewById(R.id.name);
        editText2 = findViewById(R.id.userId);
        editText3 = findViewById(R.id.userPw);
        editText5 = findViewById(R.id.gender);
        editText6 = findViewById(R.id.userPwCheck);
        profile = findViewById(R.id.memberjoin_iv);
        Button signup = findViewById(R.id.register);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoAlbum();
            }
        });
    }

//    private void gotoAlbum() {
//        startActivityForResult(
//        Intent.createChooser(
//                new Intent(Intent.ACTION_GET_CONTENT)
//                .setType("image/*"), "Choose an image"), PICK_FROM_ALBLM);
//    }

    private void gotoAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBLM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            Toast.makeText(registerActivity.this, "취소 되었습니다.", Toast.LENGTH_SHORT).show();
            if (tempFile != null) {
                if (tempFile.exists()) {
                    if (tempFile.delete()) {
                        Log.e(TAG, tempFile.getAbsolutePath() + " 삭제 성공");
                        tempFile = null;
                    }
                }
            }
            return;
        }

        switch (requestCode) {
            case PICK_FROM_ALBLM: {
                imageUri = data.getData();
                pathUri = getPath(data.getData());
                Log.d(TAG, "PICK_FROM_ALBUM photoUri : " + imageUri);
                profile.setImageURI(imageUri);
                break;
            }
        }
    }

    public String getPath(Uri uri){
        String [] proj = {MediaStore.Images.Media.DATA};
        CursorLoader cursorLoader = new CursorLoader(this, uri,proj,null,null,null);

        Cursor cursor = cursorLoader.loadInBackground();
        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();
        return cursor.getString(index);
    }

    private void signup() {
        name = editText1.getText().toString();
        String email = editText2.getText().toString();
        String emailPw = editText3.getText().toString();
        Long birth = Long.valueOf(((EditText) findViewById(R.id.birth)).getText().toString());
        String gender = editText5.getText().toString();
        String emailPwCheck = editText6.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(emailPw)) {                    // profile == null 작동을 안해서 일단 뺌
            Toast.makeText(registerActivity.this, "정보를 바르게 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!emailPw.equals(emailPwCheck) ) {
            Toast.makeText(registerActivity.this, "비밀번호가 같지 않습니다.", Toast.LENGTH_SHORT).show();
            editText3.setText("");
            editText6.setText("");
            editText3.requestFocus();
            return;
        }

        try {
            mAuth.createUserWithEmailAndPassword(email, emailPw)
                    .addOnCompleteListener(registerActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                final String uid = task.getResult().getUser().getUid();
                                final Uri file = Uri.fromFile(new File(pathUri));

                                StorageReference storageReference = mStorage.getReference().child("usersprofileImages").child("uid/"+file.getLastPathSegment());
                                storageReference.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                        final Task<Uri> imageUrl = task.getResult().getStorage().getDownloadUrl();
                                        while (!imageUrl.isComplete());

                                        UserModel userModel = new UserModel();

                                        userModel.userName = name;
                                        userModel.uid = uid;
                                        userModel.profileImageUrl = imageUrl.getResult().toString();

                                        mDatabase.getReference().child("users").child(uid).setValue(userModel);
                                    }
                                });
                                Intent intent = new Intent(registerActivity.this, loginActivity.class);
                                Toast.makeText(registerActivity.this, "회원가입에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                                startActivity(intent);
                            } else {
                                if (task.getException() != null) {
                                    Toast.makeText(registerActivity.this, "회원가입에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "createUserWithEmail: failure");
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



