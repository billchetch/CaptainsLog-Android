package net.chetch.captainslog;

public interface IDialogManager {

    public void showWarningDialog(String warning);
    public void onDialogPositiveClick(GenericDialogFragment dialog);
    public void onDialogNegativeClick(GenericDialogFragment dialog);
}
