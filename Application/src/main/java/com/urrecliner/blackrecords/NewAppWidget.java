package com.urrecliner.blackrecords;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import static com.urrecliner.blackrecords.Vars.mPackageEventPath;
import static com.urrecliner.blackrecords.Vars.utils;

public class NewAppWidget extends AppWidgetProvider {

    private static final String BIG_ICON = "BIG_ICON";
    private static final String MY_PARA = "myPara";
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        updateHomeButton(context);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {

        RemoteViews views = updateHomeButton(context);

        Intent intent = new Intent(BIG_ICON);
//        Intent intent = new Intent(context, NewAppWidget.class);
        int [] appWidgetIds = {appWidgetId};
//        Log.w("widget",AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtra(MY_PARA, BIG_ICON);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.bigIcon, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @NonNull
    public static RemoteViews updateHomeButton(Context context) {
        int cnt = utils.getDirectoryFiltered(mPackageEventPath, "mp4").length;
        String widgetText = "   ";
        if (cnt > 0)
            widgetText += cnt;
        else
            widgetText = "Ready";
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.homepage_widget);
        views.setTextViewText(R.id.homeWidget_text, widgetText);
        views.setTextViewText(R.id.homeWidget_text2, widgetText);
        widgetText = utils.getMilliSec2String(System.currentTimeMillis(), "HH:mm:SS");
        views.setTextViewText(R.id.homeWidget_time, widgetText);
        return views;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), NewAppWidget.class.getName());
        int[] appWidgets = appWidgetManager.getAppWidgetIds(thisAppWidget);

        final String action = intent.getAction();
//        Log.w("onReceive", "action:" + action);
        if(action != null && action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            Bundle extras = intent.getExtras();
            String myPara = extras.getString(MY_PARA, "");
//            Log.w("On receive"," widget update received myPARA "+MY_PARA);
            if (myPara != null && myPara.equals(BIG_ICON)) {
                Intent mainIntent = new Intent(context, MainActivity.class);
                context.startActivity(mainIntent);
//                MainActivity.widgetRun.sendEmptyMessage(0);
            }
            this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgets);
        }
    }
}

