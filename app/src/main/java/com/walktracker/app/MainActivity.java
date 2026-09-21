package com.walktracker.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.text.*;

public class MainActivity extends Activity {
    static final int REQ_EXPORT=101, REQ_IMPORT=102;
    SharedPreferences prefs;
    Calendar shown=Calendar.getInstance();
    LinearLayout calendar;
    TextView monthTitle, summary, backupStatus;
    final int COLOR_0=Color.rgb(242,242,242), COLOR_11=Color.rgb(185,230,185), COLOR_21=Color.rgb(175,200,245);
    int dp(float n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    TextView text(String s,float size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.DKGRAY); t.setGravity(Gravity.CENTER); return t; }
    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("walk_data",MODE_PRIVATE); build(); render(); }
    void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(8),dp(6),dp(8),dp(6));
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=new Button(this); prev.setText("‹"); Button next=new Button(this); next.setText("›");
        monthTitle=text("",22); monthTitle.setTypeface(null,1);
        head.addView(prev,new LinearLayout.LayoutParams(dp(58),dp(52))); head.addView(monthTitle,new LinearLayout.LayoutParams(0,dp(52),1)); head.addView(next,new LinearLayout.LayoutParams(dp(58),dp(52)));
        prev.setOnClickListener(v->{shown.add(Calendar.MONTH,-1);render();}); next.setOnClickListener(v->{shown.add(Calendar.MONTH,1);render();}); root.addView(head);
        LinearLayout legend=new LinearLayout(this); legend.setGravity(Gravity.CENTER); legend.addView(legendBox("0–10",COLOR_0)); legend.addView(legendBox("11–20",COLOR_11)); legend.addView(legendBox("21–199",COLOR_21)); root.addView(legend,new LinearLayout.LayoutParams(-1,dp(34)));
        LinearLayout week=new LinearLayout(this); String[] ds={"S","M","T","W","T","F","S"}; for(String d:ds){TextView x=text(d,13);x.setTypeface(null,1);week.addView(x,new LinearLayout.LayoutParams(0,dp(28),1));} root.addView(week);
        ScrollView scroll=new ScrollView(this);
        calendar=new LinearLayout(this); calendar.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(calendar,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        summary=text("",13); summary.setPadding(0,dp(4),0,dp(4)); root.addView(summary,new LinearLayout.LayoutParams(-1,dp(48)));
        backupStatus=text("Automatic local backup: checking…",11);
        root.addView(backupStatus,new LinearLayout.LayoutParams(-1,dp(28)));
        LinearLayout actions=new LinearLayout(this); Button backup=new Button(this);backup.setText("BACKUP");Button restore=new Button(this);restore.setText("RESTORE"); actions.addView(backup,new LinearLayout.LayoutParams(0,dp(52),1));actions.addView(restore,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(actions);
        backup.setOnClickListener(v->exportData()); restore.setOnClickListener(v->importData()); setContentView(root);
    }
    View legendBox(String s,int c){TextView t=text(s,11);t.setBackgroundColor(c);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(62),dp(26));p.setMargins(dp(2),0,dp(2),0);t.setLayoutParams(p);return t;}
    String key(int y,int m,int d){return String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);} int get(String k){return prefs.getInt(k,0);}
    int bg(int n){return n>=21?COLOR_21:n>=11?COLOR_11:COLOR_0;}
    GradientDrawable borderBg(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setStroke(dp(1),Color.LTGRAY);g.setCornerRadius(dp(2));return g;}
    void render(){
        monthTitle.setText(new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(shown.getTime()));
        calendar.removeAllViews();
        Calendar first=(Calendar)shown.clone();
        first.set(Calendar.DAY_OF_MONTH,1);
        int lead=first.get(Calendar.DAY_OF_WEEK)-1;
        int max=first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int total=lead+max;
        int rows=(total+6)/7;
        for(int r=0;r<rows;r++){
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(7f);
            calendar.addView(row,new LinearLayout.LayoutParams(-1,dp(78)));
            for(int c=0;c<7;c++){
                int i=r*7+c;
                FrameLayout cell=new FrameLayout(this);
                cell.setPadding(dp(2),dp(2),dp(2),dp(2));
                row.addView(cell,new LinearLayout.LayoutParams(0,-1,1));
                if(i<lead||i>=total) continue;
                int day=i-lead+1;
                String k=key(shown.get(Calendar.YEAR),shown.get(Calendar.MONTH),day);
                int m=get(k+"_m"), e=get(k+"_e");
                LinearLayout box=new LinearLayout(this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(dp(3),dp(2),dp(3),dp(2));
                GradientDrawable base=borderBg(Color.WHITE);
                box.setBackground(base);

                TextView d=text(""+day,12);
                d.setGravity(Gravity.LEFT|Gravity.TOP);
                d.setTypeface(null,1);
                box.addView(d,new LinearLayout.LayoutParams(-1,dp(20)));

                TextView mm=text("M  "+(m>0?m:"—"),10);
                mm.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
                mm.setPadding(dp(3),0,dp(2),0);
                mm.setBackgroundColor(bg(m));
                box.addView(mm,new LinearLayout.LayoutParams(-1,0,1));

                TextView ee=text("E  "+(e>0?e:"—"),10);
                ee.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
                ee.setPadding(dp(3),0,dp(2),0);
                ee.setBackgroundColor(bg(e));
                LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,0,1);
                ep.setMargins(0,dp(2),0,0);
                box.addView(ee,ep);

                TextView edit=text("EDIT",9);
                edit.setTextColor(Color.rgb(40,70,150));
                edit.setGravity(Gravity.CENTER);
                box.addView(edit,new LinearLayout.LayoutParams(-1,dp(17)));

                cell.addView(box,new FrameLayout.LayoutParams(-1,-1));
                View.OnClickListener l=v->editDay(k,day);
                cell.setOnClickListener(l);
                box.setOnClickListener(l);
            }
        }
        int mr=0,er=0;
        for(int d=1;d<=max;d++){
            String k=key(shown.get(Calendar.YEAR),shown.get(Calendar.MONTH),d);
            mr+=get(k+"_m"); er+=get(k+"_e");
        }
        summary.setText("Month total: Morning "+mr+" rounds  •  Evening "+er+" rounds");
    }
    void editDay(String key,int day){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10),0,dp(10),0);
        TextView info=text("Enter any number of rounds from 0 to 199",13);
        box.addView(info,new LinearLayout.LayoutParams(-1,dp(42)));
        addEditor(box,"MORNING",key+"_m");
        addEditor(box,"EVENING",key+"_e");
        new AlertDialog.Builder(this).setTitle("Day "+day).setView(box).setPositiveButton("CLOSE",null).show();
    }
    void addEditor(LinearLayout parent,String title,String k){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0,dp(4),0,dp(8));
        TextView label=text(title+"  •  "+get(k)+" rounds",15);
        label.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        row.addView(label,new LinearLayout.LayoutParams(-1,dp(42)));
        Button custom=new Button(this); custom.setText("ENTER ROUNDS (0–199)");
        row.addView(custom,new LinearLayout.LayoutParams(-1,dp(52)));
        parent.addView(row);
        custom.setOnClickListener(v->chooseRounds(k,title,label));
    }
    void chooseRounds(String k,String title,TextView label){
        LinearLayout wrap=new LinearLayout(this);
        wrap.setGravity(Gravity.CENTER);
        NumberPicker picker=new NumberPicker(this);
        picker.setMinValue(0); picker.setMaxValue(199); picker.setValue(get(k));
        wrap.addView(picker,new LinearLayout.LayoutParams(dp(110),dp(190)));
        new AlertDialog.Builder(this).setTitle(title+" rounds (0–199)").setView(wrap)
            .setPositiveButton("SAVE",(d,w)->{int n=picker.getValue(); save(k,n); label.setText(title+"  •  "+n+" rounds"); render();})
            .setNegativeButton("CANCEL",null).show();
    }
    void save(String k,int n){
        if(n<0 || n>199) return;
        if(!prefs.edit().putInt(k,n).commit()){
            showBackupStatus("Data save failed");
            return;
        }
        autoBackup();
    }
    JSONObject makeBackup() throws Exception{
        JSONObject root=new JSONObject();
        root.put("schema",1);
        root.put("savedAt",System.currentTimeMillis());
        JSONObject data=new JSONObject();
        for(String k:prefs.getAll().keySet()){
            if(k.matches("\\d{4}-\\d{2}-\\d{2}_[me]")){
                int n=prefs.getInt(k,0);
                if(n<0 || n>199) throw new Exception("Invalid round value in local data");
                data.put(k,n);
            }
        }
        root.put("data",data);
        return root;
    }
    void showBackupStatus(String s){ if(backupStatus!=null) backupStatus.setText(s); }
    void autoBackup(){
        try{
            byte[] bytes=makeBackup().toString().getBytes("UTF-8");
            // Always keep an automatic copy inside the app's private local storage.
            File dir=new File(getFilesDir(),"backup");
            if(!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create local backup folder");
            File tmp=new File(dir,"WalkTracker_AutoBackup.tmp");
            File dst=new File(dir,"WalkTracker_AutoBackup.json");
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(bytes);out.flush();out.getFD().sync();}
            if(dst.exists() && !dst.delete()) throw new IOException("Cannot replace local backup");
            if(!tmp.renameTo(dst)) throw new IOException("Cannot finalize local backup");

            // On Android 10+, also keep a user-visible copy in Downloads/WalkTracker.
            if(android.os.Build.VERSION.SDK_INT >= 29){
                ContentResolver cr=getContentResolver();
                Uri existing=null;
                String sel=MediaStore.Downloads.DISPLAY_NAME+"=? AND "+MediaStore.Downloads.RELATIVE_PATH+"=?";
                String[] args={"WalkTracker_AutoBackup.json",Environment.DIRECTORY_DOWNLOADS+"/WalkTracker/"};
                try(android.database.Cursor cur=cr.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Downloads._ID},sel,args,null)){
                    if(cur!=null && cur.moveToFirst()) existing=ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cur.getLong(0));
                }
                if(existing==null){
                    ContentValues v=new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME,"WalkTracker_AutoBackup.json");
                    v.put(MediaStore.Downloads.MIME_TYPE,"application/json");
                    v.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/WalkTracker/");
                    existing=cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
                }
                if(existing==null) throw new IOException("Cannot create Downloads backup");
                try(OutputStream out=cr.openOutputStream(existing,"wt")){
                    if(out==null) throw new IOException("Cannot open Downloads backup");
                    out.write(bytes); out.flush();
                }
            }
            showBackupStatus(android.os.Build.VERSION.SDK_INT>=29 ? "Automatic backup: local + Downloads ✓" : "Automatic backup: local ✓");
        }catch(Exception e){
            showBackupStatus("Automatic backup error: "+e.getMessage());
        }
    }
    void exportData(){
        try{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType("application/json");
            i.putExtra(Intent.EXTRA_TITLE,"WalkTracker_Backup.json");
            startActivityForResult(i,REQ_EXPORT);
        }catch(Exception e){showBackupStatus("Backup export unavailable");}
    }
    void importData(){
        try{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("application/json");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i,REQ_IMPORT);
        }catch(Exception e){showBackupStatus("Backup restore unavailable");}
    }
    boolean validDataKey(String k){return k.matches("\\d{4}-\\d{2}-\\d{2}_[me]");}
    @Override protected void onActivityResult(int r,int c,Intent data){
        super.onActivityResult(r,c,data);
        if(c!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            if(r==REQ_EXPORT){
                JSONObject all=makeBackup();
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out==null) throw new IOException("Cannot open backup file");
                    out.write(all.toString(2).getBytes("UTF-8"));
                }
                showBackupStatus("Manual backup saved ✓");
                Toast.makeText(this,"Backup saved",Toast.LENGTH_SHORT).show();
            }else if(r==REQ_IMPORT){
                StringBuilder s=new StringBuilder();
                try(InputStream in=getContentResolver().openInputStream(data.getData())){
                    if(in==null) throw new IOException("Cannot open backup file");
                    BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));
                    String line; while((line=br.readLine())!=null)s.append(line);
                }
                JSONObject root=new JSONObject(s.toString());
                JSONObject all=root.has("data")?root.getJSONObject("data"):root;
                SharedPreferences.Editor ed=prefs.edit().clear();
                Iterator<String> it=all.keys();
                while(it.hasNext()){
                    String k=it.next();
                    if(!validDataKey(k)) throw new Exception("Invalid backup format");
                    int n=all.getInt(k);
                    if(n<0||n>199) throw new Exception("Backup contains an invalid round value");
                    ed.putInt(k,n);
                }
                if(!ed.commit()) throw new IOException("Could not restore local data");
                autoBackup();
                render();
                Toast.makeText(this,"Backup restored",Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            new AlertDialog.Builder(this).setTitle("Backup error").setMessage(e.getMessage()==null?"Unknown error":e.getMessage()).setPositiveButton("OK",null).show();
        }
    }
}
