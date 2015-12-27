/**
 * This file is part of TKConfig application.
 * 
 * Copyright (C) 2015 Claudiu Ciobotariu
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.tkconfig.activities;

import ro.ciubex.tkconfig.R;
import ro.ciubex.tkconfig.TKConfigApplication;
import ro.ciubex.tkconfig.models.Constants;
import ro.ciubex.tkconfig.models.Utilities;
import ro.ciubex.tkconfig.tasks.DefaultAsyncTaskResult;
import ro.ciubex.tkconfig.tasks.PreferencesFileUtilAsynkTask;
import ro.ciubex.tkconfig.forms.CustomEditTextPreference;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

/**
 * This is the main preference activity class.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class TkPreferences extends PreferenceActivity implements
		CustomEditTextPreference.Listener,
		PreferencesFileUtilAsynkTask.Responder {
	private TKConfigApplication mApplication;
	private CustomEditTextPreference preferencesBackup;
	private CustomEditTextPreference preferencesRestore;
	private static final int PREF_BACKUP = 1;
	private static final int PREF_RESTORE = 2;
	private static final int PERMISSIONS_REQUEST_CODE = 44;

	/**
	 * Method called when this preference activity is created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApplication = (TKConfigApplication) getApplication();
		addPreferencesFromResource(R.xml.tk_preferences);
		prepareCommands();
		initPreferencesByPermissions();
		prepareAllCustomEditTextPreference();
	}

	/**
	 * Prepare preference handler
	 */
	private void prepareCommands() {
		Preference preferencesGpsContacts = (Preference) findPreference("gpsContacts");
		preferencesGpsContacts
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						return onShowGPSContacts();
					}
				});
		Preference preferencesReset = (Preference) findPreference("resetCommands");
		preferencesReset
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						return onCommandsReset();
					}
				});
		Preference requestPermissions = (Preference) findPreference("requestPermissions");
		requestPermissions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				return onRequestPermissionsDialog();
			}
		});
	}

	/**
	 * Remove the permission request preference if should not be asked for permissions.
	 */
	private void initPreferencesByPermissions() {
		if (!mApplication.shouldAskPermissions()) {
			Preference requestPermissions = (Preference) findPreference("requestPermissions");
			PreferenceCategory generalSettings = (PreferenceCategory) findPreference("generalSettings");
			generalSettings.removePreference(requestPermissions);
		}
	}

	/**
	 * Prepare some custom preferences to not be stored. The preference backup
	 * and restore are actually used as buttons.
	 */
	private void prepareAllCustomEditTextPreference() {
		String backupPath = getBackupPath();
		preferencesBackup = (CustomEditTextPreference) findPreference("preferencesBackup");
		preferencesBackup.setResultListener(this, PREF_BACKUP);
		preferencesBackup.setText(backupPath);
		preferencesRestore = (CustomEditTextPreference) findPreference("preferencesRestore");
		preferencesRestore.setResultListener(this, PREF_RESTORE);
		preferencesRestore.setText(backupPath);
	}

	/**
	 * Show the GPS contact list.
	 * 
	 * @return Always will be returned TRUE.
	 */
	private boolean onShowGPSContacts() {
		Intent intent = new Intent(getBaseContext(), GpsContactActivity.class);
		startActivityForResult(intent, 1);
		return true;
	}

	/**
	 * Method invoked when is started PreferencesFileUtilAsynkTask task
	 * 
	 * @param operationType
	 *            The operation type: backup or restore
	 */
	@Override
	public void startFileAsynkTask(
			PreferencesFileUtilAsynkTask.Operation operationType) {
		if (operationType == PreferencesFileUtilAsynkTask.Operation.RESTORE) {
			mApplication.showProgressDialog(this, R.string.backup_started);
		} else {
			mApplication.showProgressDialog(this, R.string.restore_started);
		}
	}

	/**
	 * Method invoked when is ended PreferencesFileUtilAsynkTask task
	 * 
	 * @param operationType
	 *            The operation type: backup or restore
	 * @param result
	 *            The process result
	 */
	@Override
	public void endFileAsynkTask(
			PreferencesFileUtilAsynkTask.Operation operationType,
			DefaultAsyncTaskResult result) {
		mApplication.hideProgressDialog();
		if (result.resultId == Constants.OK) {
			mApplication.showMessageInfo(this, result.resultMessage);
			if (operationType == PreferencesFileUtilAsynkTask.Operation.RESTORE) {
				// restartPreferencesActivity();
			}
		} else {
			mApplication.showMessageError(this, result.resultMessage);
		}
	}

	/**
	 * Method invoked when is pressed the positive (OK) button from a preference
	 * edit dialog
	 * 
	 * @param resultId
	 *            The id of pressed preference: PREF_RESTORE or PREF_BACKUP
	 * @param value
	 *            The full file and path of saved or loaded preferences
	 */
	@Override
	public void onPositiveResult(int resultId, String value) {
		if (resultId > 0) {
			storeBackupPath(value);
		}
		if (resultId == PREF_RESTORE) {
			preferencesBackup.setText(value);
			onRestorePreferences(value);
		} else if (resultId == PREF_BACKUP) {
			preferencesRestore.setText(value);
			onBackupPreferences(value);
		}
	}

	/**
	 * Persist into preferences the full file and path of saved or loaded
	 * preferences
	 * 
	 * @param backupPath
	 *            The full file and path of saved or loaded preferences
	 */
	private void storeBackupPath(String backupPath) {
		mApplication.setBackupPath(backupPath);
	}

	/**
	 * Obtain the full file and path of saved or loaded preferences
	 * 
	 * @return The full file and path of saved or loaded preferences
	 */
	private String getBackupPath() {
		return mApplication.getBackupPath();
	}

	/**
	 * Method invoked when is pressed the negative (Cancel) button from a
	 * preference edit dialog
	 * 
	 * @param resultId
	 *            The id of pressed preference: PREF_RESTORE or PREF_BACKUP
	 * @param value
	 *            The full file and path of saved or loaded preferences
	 */
	@Override
	public void onNegativeResult(int resultId, String value) {

	}

	/**
	 * Method invoked when is pressed the restore preference This will launch a
	 * PreferencesFileUtilAsynkTask task to restore preferences
	 * 
	 * @param backupPath
	 *            The full file and path from where should be loaded preferences
	 */
	private void onRestorePreferences(String backupPath) {
		new PreferencesFileUtilAsynkTask(this, backupPath,
				PreferencesFileUtilAsynkTask.Operation.RESTORE).execute();
	}

	/**
	 * Method invoked when is pressed the back-up preference This will launch
	 * PreferencesFileUtilAsynkTask task to backup preferences
	 * 
	 * @param backupPath
	 *            The full file and path where should be stored preferences
	 */
	private void onBackupPreferences(String backupPath) {
		new PreferencesFileUtilAsynkTask(this, backupPath,
				PreferencesFileUtilAsynkTask.Operation.BACKUP).execute();
	}

	/**
	 * Show a reset command question dialog window.
	 * 
	 * @return Always will be returned TRUE.
	 */
	private boolean onCommandsReset() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.reset_commands_question)
				.setMessage(R.string.reset_commands_question_desc)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								doCommandsReset();
							}
						}).setNegativeButton(R.string.no, null).show();
		return true;
	}

	/**
	 * This method is used to reset the command list to default commands.
	 */
	private void doCommandsReset() {
		mApplication.populateDefaultCommands();
		mApplication.commandsStoreCleanup();
		mApplication.setMustReloadCommands(true);
	}

	/**
	 * Method invoked when was pressed the request permission preference.
	 */
	private boolean onRequestPermissionsDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setMessage(R.string.request_permissions_confirmation)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
												int whichButton) {
								doRequestPermissions();
							}
						}).setNegativeButton(R.string.no, null).show();
		return true;
	}

	/**
	 * Check if are permissions needed to be requested.
	 */
	private void doRequestPermissions() {
		String[] permissions = mApplication.getNotGrantedPermissions();
		if (Utilities.isEmpty(permissions)) {
			new AlertDialog.Builder(this)
					.setTitle(R.string.app_name)
					.setMessage(R.string.request_permissions_ok)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setNeutralButton(R.string.ok, null).show();
		} else {
			requestForPermissions(permissions);
		}
	}

	/**
	 * Method used to request for application required permissions.
	 */
	@TargetApi(23)
	private void requestForPermissions(String[] permissions) {
		if (!Utilities.isEmpty(permissions)) {
			requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
		}
	}

	/**
	 * Callback for the result from requesting permissions.
	 *
	 * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
	 * @param permissions  The requested permissions. Never null.
	 * @param grantResults The grant results for the corresponding permissions.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (PERMISSIONS_REQUEST_CODE == requestCode) {
			mApplication.markPermissionsAsked();
			for (String permission : permissions) {
				mApplication.markPermissionAsked(permission);
			}
		}
	}
}
