package com.walktracker.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
    GridLayout calendar;
    TextView monthTitle, summary;
    final int BLUE=Color.rgb(175,200,245), GREEN=Color.rgb(185,230,185), YELLOW=Color.rgb(255,240,166), GREY=Color.rgb(242,242,242);
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
        LinearLayout legend=new LinearLayout(this); legend.setGravity(Gravity.CENTER); legend.addView(legendBox("0",GREY)); legend.addView(legendBox("1–9",YELLOW)); legend.addView(legendBox("10–19",GREEN)); legend.addView(legendBox("20+",BLUE)); root.addView(legend,new LinearLayout.LayoutParams(-1,dp(34)));
        LinearLayout week=new LinearLayout(this); String[] ds={"S","M","T","W","T","F","S"}; for(String d:ds){TextView x=text(d,13);x.setTypeface(null,1);week.addView(x,new LinearLayout.LayoutParams(0,dp(28),1));} root.addView(week);
        calendar=new GridLayout(this); calendar.setColumnCount(7); calendar.setUseDefaultMargins(false); root.addView(calendar,new LinearLayout.LayoutParams(-1,0,1));
        summary=text("",13); summary.setPadding(0,dp(4),0,dp(4)); root.addView(summary,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout actions=new LinearLayout(this); Button backup=new Button(this);backup.setText("BACKUP");Button restore=new Button(this);restore.setText("RESTORE"); actions.addView(backup,new LinearLayout.LayoutParams(0,dp(52),1));actions.addView(restore,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(actions);
        backup.setOnClickListener(v->exportData()); restore.setOnClickListener(v->importData()); setContentView(root);
    }
    View legendBox(String s,int c){TextView t=text(s,11);t.setBackgroundColor(c);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(62),dp(26));p.setMargins(dp(2),0,dp(2),0);t.setLayoutParams(p);return t;}
    String key(int y,int m,int d){return String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);} int get(String k){return prefs.getInt(k,0);}
    int bg(int n){return n>=20?BLUE:n>=10?GREEN:n>0?YELLOW:GREY;}
    GradientDrawable borderBg(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setStroke(dp(1),Color.LTGRAY);g.setCornerRadius(dp(2));return g;}
    void render(){
        monthTitle.setText(new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(shown.getTime())); calendar.removeAllViews();
        Calendar first=(Calendar)shown.clone();first.set(Calendar.DAY_OF_MONTH,1);int lead=first.get(Calendar.DAY_OF_WEEK)-1;int max=first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int total=lead+max; int rows=(total+6)/7;
        int cellH=Math.max(dp(72),dp(380)/rows);
        for(int i=0;i<rows*7;i++){
            FrameLayout cell=new FrameLayout(this); cell.setPadding(dp(2),dp(2),dp(2),dp(2));
            GridLayout.LayoutParams gp=new GridLayout.LayoutParams(); gp.width=0; gp.height=cellH; gp.columnSpec=GridLayout.spec(i%7,1f); gp.rowSpec=GridLayout.spec(i/7,1f); calendar.addView(cell,gp);
            if(i<lead||i>=total) continue;
            int day=i-lead+1; String k=key(shown.get(Calendar.YEAR),shown.get(Calendar.MONTH),day); int m=get(k+"_m"),e=get(k+"_e");
            LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(3),dp(3),dp(3),dp(3));box.setBackground(borderBg(bg(Math.max(m,e))));
            TextView d=text(""+day,13);d.setGravity(Gravity.LEFT|Gravity.TOP);d.setTypeface(null,1);box.addView(d,new LinearLayout.LayoutParams(-1,dp(22)));
            TextView mm=text("M  "+(m>0?m+" R":"—"),11);TextView ee=text("E  "+(e>0?e+" R":"—"),11);mm.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);ee.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);box.addView(mm,new LinearLayout.LayoutParams(-1,0,1));box.addView(ee,new LinearLayout.LayoutParams(-1,0,1));
            TextView edit=text("EDIT",10);edit.setTextColor(Color.rgb(40,70,150));box.addView(edit,new LinearLayout.LayoutParams(-1,dp(18)));
            cell.addView(box,new FrameLayout.LayoutParams(-1,-1)); View.OnClickListener l=v->editDay(k,day);box.setOnClickListener(l);cell.setOnClickListener(l);
        }
        int mr=0,er=0;for(int d=1;d<=max;d++){String k=key(shown.get(Calendar.YEAR),shown.get(Calendar.MONTH),d);mr+=get(k+"_m");er+=get(k+"_e");}summary.setText("Month total: Morning "+mr+" rounds ("+(mr*250)+" steps)  •  Evening "+er+" rounds ("+(er*250)+" steps)");
    }
    void editDay(String key,int day){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(10),0,dp(10),0);TextView info=text("250 steps = 1 round",13);box.addView(info,new LinearLayout.LayoutParams(-1,dp(34)));
        addEditor(box,"MORNING",key+"_m");addEditor(box,"EVENING",key+"_e");
        new AlertDialog.Builder(this).setTitle("Day "+day).setView(box).setPositiveButton("CLOSE",null).show();
    }
    void addEditor(LinearLayout parent,String title,String k){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=text(title,15);label.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);row.addView(label,new LinearLayout.LayoutParams(0,dp(58),1));
        Button ten=new Button(this);ten.setText("10");Button twenty=new Button(this);twenty.setText("20");Button minus=new Button(this);minus.setText("−");Button count=new Button(this);count.setText(""+get(k));Button plus=new Button(this);plus.setText("+");
        row.addView(ten,new LinearLayout.LayoutParams(dp(52),dp(52)));row.addView(twenty,new LinearLayout.LayoutParams(dp(52),dp(52)));row.addView(minus,new LinearLayout.LayoutParams(dp(44),dp(52)));row.addView(count,new LinearLayout.LayoutParams(dp(58),dp(52)));row.addView(plus,new LinearLayout.LayoutParams(dp(44),dp(52)));parent.addView(row);
        ten.setOnClickListener(v->{save(k,10);count.setText("10");render();});twenty.setOnClickListener(v->{save(k,20);count.setText("20");render();});minus.setOnClickListener(v->{int n=Math.max(0,get(k)-1);save(k,n);count.setText(""+n);render();});plus.setOnClickListener(v->{int n=get(k)+1;save(k,n);count.setText(""+n);render();});
    }
    void save(String k,int n){prefs.edit().putInt(k,n).apply();}
    void exportData(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"WalkingTracker_Backup.json");startActivityForResult(i,REQ_EXPORT);}
    void importData(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_IMPORT);}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(c!=RESULT_OK||data==null)return;try{if(r==REQ_EXPORT){JSONObject all=new JSONObject();for(String k:prefs.getAll().keySet())all.put(k,prefs.getAll().get(k));try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(all.toString().getBytes("UTF-8"));}Toast.makeText(this,"Backup saved",Toast.LENGTH_SHORT).show();}else{try(InputStream in=getContentResolver().openInputStream(data.getData())){BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));StringBuilder s=new StringBuilder();String line;while((line=br.readLine())!=null)s.append(line);JSONObject all=new JSONObject(s.toString());SharedPreferences.Editor ed=prefs.edit().clear();Iterator<String> it=all.keys();while(it.hasNext()){String k=it.next();ed.putInt(k,all.getInt(k));}ed.apply();render();Toast.makeText(this,"Backup restored",Toast.LENGTH_SHORT).show();}}}catch(Exception e){new AlertDialog.Builder(this).setTitle("Backup error").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}}
}
