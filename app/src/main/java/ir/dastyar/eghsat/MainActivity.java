package ir.dastyar.eghsat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, list;
    TextView summary, empty;
    final int REQ = 90;

    int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    TextView tv(String s, float size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(dp(12),dp(8),dp(12),dp(8)); return t;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(247,247,250));
        build();
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ);
    }

    void build() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247,247,250));
        root.setPadding(dp(16),dp(12),dp(16),dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = tv("دستیار اقساط", 25, Color.rgb(23,23,28));
        title.setTypeface(null, 1);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button add = new Button(this); add.setText("+ قسط جدید");
        add.setOnClickListener(v -> startActivity(new Intent(this, AddInstallmentActivity.class)));
        top.addView(add);
        root.addView(top);

        summary = tv("", 16, Color.rgb(91,75,219));
        summary.setBackgroundColor(Color.WHITE);
        root.addView(summary, new LinearLayout.LayoutParams(-1, dp(88)));

        TextView h = tv("اقساط من", 20, Color.rgb(23,23,28)); h.setTypeface(null,1);
        root.addView(h);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    String money(long n) { return NumberFormat.getInstance(Locale.US).format(n) + " تومان"; }

    void refresh() {
        list.removeAllViews();
        List<Installment> xs = Store.all(this);
        long total = 0; int open=0;
        for (Installment x: xs) if (!x.isFinished()) { total += x.amount * (x.totalCount-x.paidCount); open++; }
        summary.setText("  " + open + " قسط فعال\n  مانده تقریبی: " + money(total));

        if (xs.isEmpty()) {
            TextView e = tv("هنوز قسطی ثبت نکرده‌ای.\nاز «+ قسط جدید» شروع کن.", 17, Color.DKGRAY);
            e.setGravity(Gravity.CENTER); list.addView(e, new LinearLayout.LayoutParams(-1,dp(180)));
            return;
        }

        Collections.sort(xs, (a,b)->Long.compare(a.nextDueMillis(), b.nextDueMillis()));
        for (Installment x: xs) addCard(x);
    }

    void addCard(Installment x) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(10),dp(14),dp(10)); card.setBackgroundColor(Color.WHITE);
        TextView name = tv(x.title, 18, Color.rgb(23,23,28)); name.setTypeface(null,1);
        card.addView(name);
        card.addView(tv("قسط " + Math.min(x.paidCount+1,x.totalCount) + " از " + x.totalCount +
                "  •  مبلغ: " + money(x.amount), 15, Color.DKGRAY));
        card.addView(tv(x.isFinished() ? "تکمیل شده" : "سررسید: " + x.dueText(), 14,
                x.isFinished()?Color.rgb(22,138,88):Color.rgb(91,75,219)));

        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        Button pay = new Button(this); pay.setText(x.isFinished() ? "تکمیل شد" : "✓ ثبت پرداخت");
        pay.setEnabled(!x.isFinished());
        pay.setOnClickListener(v -> {
            x.paidCount++;
            if (x.paidCount >= x.totalCount) x.active = false;
            Store.upsert(this,x);
            AlarmHelper.schedule(this,x);
            refresh();
        });
        Button edit = new Button(this); edit.setText("ویرایش");
        edit.setOnClickListener(v -> startActivity(new Intent(this,AddInstallmentActivity.class).putExtra("id",x.id)));
        row.addView(pay, new LinearLayout.LayoutParams(0,-2,1)); row.addView(edit);
        card.addView(row);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0,dp(8),0,0); list.addView(card,lp);
    }
}
