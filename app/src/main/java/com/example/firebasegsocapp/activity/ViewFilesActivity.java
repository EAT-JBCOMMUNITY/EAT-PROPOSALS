package com.example.firebasegsocapp.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.firebasegsocapp.adapter.FilesAdapter;
import com.example.firebasegsocapp.domain.FirebaseFile;
import com.example.firebasegsocapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;

import java.util.*;

import static com.example.firebasegsocapp.activity.MainActivity.getFirebaseAuth;
import static com.example.firebasegsocapp.activity.MainActivity.getStorageReference;

public class ViewFilesActivity extends AppCompatActivity {

    private static int filesToParse;
    private static int parsedFiles = 0;
    private static ProgressDialog progressDialog;
    private static String viewType = "list";
    private final int LOGIN_ACTIVITY_CODE = 1;

    private StorageReference storageReference;
    private List<FirebaseFile> firebaseFiles;
    private RecyclerView rvFiles;
    private FilesAdapter adapter;

    private TextView txtViewLogin;
    private TextView txtViewSortFiles;
    private TextView txtViewChangeViewFiles;
    private RelativeLayout layoutFileOptions;

    private final String[] sortItems = new String[]{"File Name", "File Size", "Upload Date", "File Type"};
    private final int[] checkedItem = {-1};

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);

        init();
        setListeners();
        updateRegisterDependentElements();
        getFilesFromFirebase();
    }

    private void init(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading files...");
        storageReference = getStorageReference();
        firebaseFiles= new ArrayList<>();
        rvFiles = findViewById(R.id.rvFiles);
        txtViewLogin = findViewById((R.id.txtViewLogin));
        txtViewSortFiles = findViewById(R.id.txtViewSortFiles);
        txtViewChangeViewFiles = findViewById(R.id.txtViewChangeFilesView);
        layoutFileOptions = findViewById(R.id.layoutNoFilesFound);
    }

    private void setListeners(){
        txtViewSortFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ViewFilesActivity.this);
                builder.setTitle("Sort files by:");
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setSingleChoiceItems(sortItems, checkedItem[0], (dialog, which) -> {
                    checkedItem[0] = which;
                    sortFiles(which);
                    checkedItem[0]=-1;
                    dialog.dismiss();
                });
                builder.setNegativeButton("Cancel", (dialog,which)->{
                    checkedItem[0]=-1;
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        txtViewChangeViewFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewType.equals("list")){
                    txtViewChangeViewFiles.setText("List View");
                    viewType = "grid";
                    configureRecyclerView();
                    return;
                }
                txtViewChangeViewFiles.setText("Grid View");
                viewType = "list";
                configureRecyclerView();
            }
        });
    }

    private void updateRegisterDependentElements(){
        if(getFirebaseAuth().getCurrentUser() != null && getFirebaseAuth().getCurrentUser().isEmailVerified()) {
            txtViewLogin.setText("Logout");
            txtViewLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getFirebaseAuth().signOut();
                    updateRegisterDependentElements();
                }
            });
            return;
        }

        txtViewLogin.setText("Sign Up | Sign In");
        txtViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Use custom form for registering
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivityForResult(intent, LOGIN_ACTIVITY_CODE);
            }
        });

    }

    private void getFilesFromFirebase(){
        progressDialog.show();
        storageReference.child("accepted-files").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                filesToParse = listResult.getItems().size();
                if(filesToParse==0){
                    txtViewSortFiles.setEnabled(false);
                    txtViewSortFiles.setTextColor(getResources().getColor(R.color.cultured));
                    txtViewChangeViewFiles.setEnabled(false);
                    txtViewChangeViewFiles.setTextColor(getResources().getColor(R.color.cultured));
                    layoutFileOptions.setVisibility(View.VISIBLE);

                    progressDialog.dismiss();
                    return;
                }
                for(StorageReference fileReference : listResult.getItems()) {
                    getFileMetadata(fileReference);
                }

            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @NotNull Exception e) {
            }
        });
    }

    private void getFileMetadata(StorageReference fileReference){
        String filePath = fileReference.getPath();
        String fileName = filePath.substring(filePath.lastIndexOf("/" )+1, filePath.lastIndexOf("."));
        String fileType = filePath.substring(filePath.lastIndexOf("."));

        fileReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                String fileSize = getFileSize(storageMetadata.getSizeBytes());
                String fileUploadTime = getDateFromMilliseconds(storageMetadata.getCreationTimeMillis());

                firebaseFiles.add(new FirebaseFile(filePath, fileName, fileType, fileSize, fileUploadTime));
                parsedFiles++;
                if(parsedFiles == filesToParse)
                    configureRecyclerView();
            }
        });
    }

    private void configureRecyclerView(){
        if(viewType.equals("list"))
            rvFiles.setLayoutManager(new GridLayoutManager(this, 1));
        else
            rvFiles.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new FilesAdapter(firebaseFiles, viewType);
        rvFiles.setAdapter(adapter);
        progressDialog.dismiss();
    }

    private void sortFiles(int sortSelection){

        switch (sortSelection){
            case 0:
                Toast.makeText(this, "Sorting Name", Toast.LENGTH_SHORT).show();
                Collections.sort(firebaseFiles, new Comparator<FirebaseFile>() {
                    @Override
                    public int compare(FirebaseFile file1, FirebaseFile file2) {
                        return file1.getFileName().compareTo(file2.getFileName());
                    }
                });
                adapter.notifyDataSetChanged();
                break;
            case 1:
                Toast.makeText(this, "Sorting Size", Toast.LENGTH_SHORT).show();
                Collections.sort(firebaseFiles, new Comparator<FirebaseFile>() {
                    @Override
                    public int compare(FirebaseFile file1, FirebaseFile file2) {
                        return file1.getFileSize().compareTo(file2.getFileSize());
                    }
                });
                adapter.notifyDataSetChanged();
                break;
            case 2:
                Collections.sort(firebaseFiles, new Comparator<FirebaseFile>(){
                    @Override
                    public int compare(FirebaseFile file1, FirebaseFile file2){
                        return file1.getCreationTime().compareTo(file2.getCreationTime());
                    }
                });
                adapter.notifyDataSetChanged();
                break;
            case 3:
                Toast.makeText(this, "Sorting Type", Toast.LENGTH_SHORT).show();
                Collections.sort(firebaseFiles, new Comparator<FirebaseFile>() {
                    @Override
                    public int compare(FirebaseFile file1, FirebaseFile file2) {
                        return file1.getFileType().compareTo(file2.getFileType());
                    }
                });
                adapter.notifyDataSetChanged();
                break;
        }
    }

    private String getDateFromMilliseconds(long milliseconds){
        Instant instantFromEpochMilli = Instant.ofEpochMilli(milliseconds);
        int year = instantFromEpochMilli.get(DateTimeFieldType.year());
        int month = instantFromEpochMilli.get(DateTimeFieldType.monthOfYear());
        int day = instantFromEpochMilli.get(DateTimeFieldType.dayOfMonth());
        int hour = instantFromEpochMilli.get(DateTimeFieldType.hourOfDay());
        int minute = instantFromEpochMilli.get(DateTimeFieldType.minuteOfHour());

        return day + "/" + month + "/" + year + " " + hour + ":" + minute;
    }
    
    private String getFileSize(long bytes){
        Formatter fm=new Formatter();
        if (bytes / 1024.0 < 1)
            return bytes + "bytes";
        else if(bytes / (1024.0*1024.0) < 1)
            return fm.format("%.2f", bytes / 1024.0) + "kB";
        else if(bytes / (1024.0*1024.0*1024.0) < 1)
            return fm.format("%.2f", bytes / (1024.0*1024.0)) + "MB";
        return fm.format("%.2f", bytes / (1024.0*1024.0*1024.0)) + "GB";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LOGIN_ACTIVITY_CODE:
                if (resultCode == RESULT_OK)
                    updateRegisterDependentElements();
                break;
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        filesToParse = 0;
        parsedFiles = 0;
    }
}
