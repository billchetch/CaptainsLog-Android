package net.chetch.captainslog;

import android.content.Context;
import android.content.Intent;
import net.chetch.utilities.UncaughtExceptionHandler;

public class UCEHandler extends UncaughtExceptionHandler {

    public UCEHandler(Context context, String logFile) {
        super(context, logFile);

    }

    @Override
    protected Intent buildIntent(){
        Intent intent = super.buildIntent();
        intent.putExtra("crashed", "yep crashed");
        return intent;
    }

    @Override
    public String getErrorReport(Thread thread, Throwable exception){
        String errorReport = super.getErrorReport(thread, exception);

        StringBuilder xtras = new StringBuilder();

        return errorReport + xtras.toString();
    }
}
