package net.chetch.captainslog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import net.chetch.appframework.GenericDialogFragment;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.webservices.employees.Employee;

public class ExcessOnDutyDialogFragment extends GenericDialogFragment {

    private LogEntry.XSDutyReason selectedReason = null;
    public String reason = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        inflateContentView(R.layout.excess_on_duty_dialog);

        RadioGroup rg = contentView.findViewById(R.id.reasons);
        TextView tv = contentView.findViewById(R.id.otherReason);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                String s = getResources().getResourceEntryName(i);
                selectedReason = LogEntry.XSDutyReason.valueOf(s);
                switch(selectedReason){
                    case OTHER_REASON:
                        tv.setVisibility(View.VISIBLE);
                        tv.requestFocus();
                        break;

                    default:
                        tv.setVisibility(View.GONE);
                        break;
                }

                Log.i("XSDUTY", "Checked " + selectedReason);
            }
        });

        //set the close button
        Button cancelButton = contentView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        //set the close button
        Button okButton = contentView.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveReason();
            }
        });


        Dialog dialog = createDialog();

        Log.i("XSDUTY", "Dialog created");

        return dialog;
    }

    public void saveReason(){

        LogEntry latestLogEntry = ((MainActivity)getActivity()).model.getLatestLogEntry();

        if(selectedReason == null){
            dialogManager.showWarningDialog(getString(R.string.dialog_warning_choose_event));
            return;
        }
        if(latestLogEntry == null || latestLogEntry.getEmployeeID() == null){
            dialogManager.showWarningDialog(getString(R.string.dialog_warning_choose_crew));
            return;
        }

        if(selectedReason == LogEntry.XSDutyReason.OTHER_REASON){
            TextView tv = contentView.findViewById(R.id.otherReason);
            reason = tv.getText() == null ? null : tv.getText().toString().trim();
            if(reason == null || reason.length() == 0) {
                dialogManager.showWarningDialog(getString(R.string.dialog_warning_add_comment));
                return;
            }
        } else {
            String resourceName = "label.excess_duty." + selectedReason.toString();
            reason = getString(getResourceID(resourceName, "string"));
        }

        dismiss();

        dialogManager.onDialogPositiveClick(this);
    }
}
