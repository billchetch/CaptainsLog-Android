package net.chetch.captainslog;

import android.content.Context;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;

public class GenericDialogFragment extends AppCompatDialogFragment {

    protected View contentView;
    protected IDialogManager dialogManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        dialogManager = (IDialogManager)context;
    }
}
