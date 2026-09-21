package com.walktracker.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ_EXPORT=101, REQ_IMPORT=102;
    SharedPreferences prefs; Calendar shown = Calendar.getInstance(); LinearLayout calendarGrid; TextView monthTitle, summary;
    String[] dayNames={"S","M","T","W","T","F","S"};
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("walk_data",MODE_PRIVATE); buildUI(); render();}
    TextView tv(String text,float size){TextView t=new TextView(this); t.setText(text); t.setTextSize(size); t.setGravity(Gravity.CENTER); t.setTextColor(Color.rgb(30,30,30)); return t;}
    void buildUI(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(8),dp(12),dp(8));
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=new Button(this); prev.setText("‹"); Button next=new Button(this); next.setText("›"); monthTitle=tv("",20); monthTitle.setTypeface(null,1);
        head.addView(prev,new LinearLayout.LayoutParams(dp(55),dp(50))); head.addView(monthTitle,new LinearLayout.LayoutParams(0,dp(50),1)); head.addView(next,new LinearLayout.LayoutParams(dp(55),dp(50)));
        prev.setOnClickListener(v->{shown.add(Calendar.MONTH,-1);render();}); next.setOnClickListener(v->{shown.add(Calendar.MONTH,1);render();}); root.addView(head);
        LinearLayout legend=new LinearLayout(this); legend.setGravity(Gravity.CENTER); legend.addView(legend("0",Color.rgb(241,241,241))); legend.addView(legend("1–9",Color.rgb(255,240,166))); legend.addView(legend("10–19",Color.rgb(185,230,185))); legend.addView(legend("20+",Color.rgb(175,200,245))); root.addView(legend,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout weekdays=new LinearLayout(this); for(String d:dayNames){TextView x=tv(d,12); x.setTypeface(null,1); weekdays.addView(x,new LinearLayout.LayoutParams(0,dp(28),1));} root.addView(weekdays);
        calendarGrid=new LinearLayout(this); calendarGrid.setOrientation(LinearLayout.VERTICAL); root.addView(calendarGrid,new LinearLayout.LayoutParams(-1,0,1));
        summary=tv("",14); summary.setPadding(0,dp(8),0,dp(4)); root.addView(summary);
        LinearLayout bottom=new LinearLayout(this); bottom.setGravity(Gravity.CENTER); Button backup=new Button(this); backup.setText("Backup"); Button restore=new Button(this); restore.setText("Restore"); bottom.addView(backup,new LinearLayout.LayoutParams(0,dp(48),1)); bottom.addView(restore,new LinearLayout.LayoutParams(0,dp(48),1)); root.addView(bottom);
        backup.setOnClickListener(v->exportData()); restore.setOnClickListener(v->importData()); setContentView(root);
    }
    View legend(String s,int c){TextView t=tv(s,11); t.setBackgroundColor(c); t.setPadding(dp(7),0,dp(7),0); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(60),dp(25)); p.setMargins(dp(2),0,dp(2),0); t.setLayoutParams(p); return t;}
    void render(){
        monthTitle.setText(new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(shown.getTime())); calendarGrid.removeAllViews();
        Calendar first=(Calendar)shown.clone(); first.set(Calendar.DAY_OF_MONTH,1); int lead=first.get(Calendar.DAY_OF_WEEK)-1; int max=first.getActualMaximum(Calendar.DAY_OF_MONTH); int cells=lead+max; int rows=(cells+6)/7;
        for(int r=0;r<rows;r++){LinearLayout row=new LinearLayout(this); for(int c=0;c<7;c++){int idx=r*7+c; if(idx<lead||idx>=cells){Space sp=new Space(this); row.addView(sp,new LinearLayout.LayoutParams(0,0,1));} else {int day=idx-lead+1; row.addView(dayCell(day),new LinearLayout.LayoutParams(0,0,1));}} calendarGrid.addView(row,new LinearLayout.LayoutParams(-1,0,1));}
        updateSummary(max);
    }
    TextView dayCell(int day){
        String key=key(shown.get(Calendar.YEAR),shown.get(Calendar.MONTH),day); int m=get(key+"_m"),e=get(key+"_e");
        TextView t=tv(day+"\nM "+m+"  E "+e,11); t.setPadding(1,dp(4),1,dp(2)); t.setBackgroundColor(dayColor(Math.max(m,e))); t.setOnClickListener(v->editDay(key,day));
        Calendar now=Calendar.getInstance(); if(now.get(Calendar.YEAR)==shown.get(Calendar.YEAR)&&now.get(Calendar.MONTH)==shown.get(Calendar.MONTH)&&now.get(Calendar.DAY_OF_MONTH)==day){t.setTypeface(null,1); t.setTextColor(Color.BLACK);} return t;
    }
    int dayColor(int n){if(n>=20)return Color.rgb(175,200,245); if(n>=10)return Color.rgb(185,230,185); if(n>0)return Color.rgb(255,240,166); return Color.rgb(241,241,241);}
    String key(int y,int mo,int d){return String.format(Locale.US,"%04d-%02d-%02d",y,mo+1,d);} int get(String k){return prefs.getInt(k,0);}
    void editDay(String key,int day){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(8),dp(18),0);
        TextView note=tv("250 steps = 1 round",13); note.setPadding(0,0,0,dp(8)); box.addView(note);
        addWalkEditor(box,"Morning",key+"_m"); addWalkEditor(box,"Evening",key+"_e");
        new AlertDialog.Builder(this).setTitle("Day " + day).setView(box).setPositiveButton("Done",null).show();
    }
    void addWalkEditor(LinearLayout box,String name,String dataKey){
        LinearLayout line=new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL); TextView label=tv(name,17); label.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT); line.addView(label,new LinearLayout.LayoutParams(0,dp(54),1));
        Button minus=new Button(this); minus.setText("−"); Button count= new Button(this); count.setText(""+get(dataKey)); Button plus=new Button(this); plus.setText("+"); Button ten=new Button(this); ten.setText("10"); Button twenty=new Button(this); twenty.setText("20");
        line.addView(minus,new LinearLayout.LayoutParams(dp(45),dp(50))); line.addView(count,new LinearLayout.LayoutParams(dp(60),dp(50))); line.addView(plus,new LinearLayout.LayoutParams(dp(45),dp(50))); line.addView(ten,new LinearLayout.LayoutParams(dp(55),dp(50))); line.addView(twenty,new LinearLayout.LayoutParams(dp(55),dp(50))); box.addView(line);
        View.OnClickListener set=v->{int n=((Button)v).getText().toString().equals("10")?10:20; save(dataKey,n); count.setText(""+n); render();};
        ten.setOnClickListener(set); twenty.setOnClickListener(set); minus.setOnClickListener(v->{int n=Math.max(0,get(dataKey)-1);save(dataKey,n);count.setText(""+n);render();}); plus.setOnClickListener(v->{int n=get(dataKey)+1;save(dataKey,n);count.setText(""+n);render();});
    }
    void save(String k,int n){prefs.edit().putInt(k,n).apply();}
    void updateSummary(int max){int mr=0,er=0,md=0,ed=0; for(int d=1;d<=max;d++){String k=key(shown.get(Calendar.YEAR),shown.get(Calendar.MONTH),d);int m=get(k+"_m"),e=get(k+"_e");mr+=m;er+=e;if(m>0)md++;if(e>0)ed++;} summary.setText("Month total: Morning "+mr+" rounds ("+(mr*250)+" steps)  •  Evening "+er+" rounds ("+(er*250)+" steps)");}
    void exportData(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"WalkingTracker_Backup.json");startActivityForResult(i,REQ_EXPORT);}
    void importData(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_IMPORT);}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(c!=RESULT_OK||data==null)return;try{if(r==REQ_EXPORT){JSONObject all=new JSONObject();Map<String,?> map=prefs.getAll();for(String k:map.keySet())all.put(k,map.get(k));try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(all.toString().getBytes("UTF-8"));}Toast.makeText(this,"Backup saved",Toast.LENGTH_SHORT).show();}else{try(InputStream in=getContentResolver().openInputStream(data.getData())){BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));StringBuilder s=new StringBuilder();String line;while((line=br.readLine())!=null)s.append(line);JSONObject all=new JSONObject(s.toString());SharedPreferences.Editor ed=prefs.edit();ed.clear();Iterator<String> it=all.keys();while(it.hasNext()){String k=it.next();ed.putInt(k,all.getInt(k));}ed.apply();render();Toast.makeText(this,"Backup restored",Toast.LENGTH_SHORT).show();}}}catch(Exception e){new AlertDialog.Builder(this).setTitle("Backup error").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}}
}
