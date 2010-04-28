/*******************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.designer.data.ui.dataset;

import java.util.Iterator;

import org.eclipse.birt.report.designer.data.ui.property.AbstractDescriptionPropertyPage;
import org.eclipse.birt.report.designer.nls.Messages;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.elements.interfaces.IOdaDataSetModel;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 */

public class DataSetSettingsPage extends AbstractDescriptionPropertyPage
{

	private static String DEFAULT_MESSAGE = Messages.getString( "dataset.editor.settings" ); //$NON-NLS-1$
	private transient Button fetchAllDataCheckBox = null;
	private transient Button selectResultSetCheckBox = null;
	private transient Button resultSetName = null;
	private transient Button resultSetNumber = null;
	private String numberText = null;
	private String nameText = null;
	boolean changed = false;
	
	private static String STORED_PROCEDURE_EXTENSION_ID = "org.eclipse.birt.report.data.oda.jdbc.SPSelectDataSet";
	
	public Control createContents( Composite parent )
	{
		Composite composite = new Composite( parent, SWT.NONE );

		GridLayout layout = new GridLayout( );
		layout.numColumns = 1;
		composite.setLayout( layout );
		composite.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_FILL ) );

		Object handle = ( (DataSetEditor) getContainer( ) ).getHandle( );
		if ( handle instanceof OdaDataSetHandle )
		{
			addDataFetchSettingGroup( composite );
			
			String extensionID = ( (OdaDataSetHandle) handle ).getExtensionID( );
			if ( extensionID != null
					&& extensionID.equalsIgnoreCase( STORED_PROCEDURE_EXTENSION_ID ) )
			{
				addResultSetGroup( composite );
			}
		}
		return composite;
	}

	/**
	 * Add row fetch limit control group.
	 * 
	 * @param composite
	 */
	private void addDataFetchSettingGroup( Composite composite )
	{
		GridLayout groupGridLayout = new GridLayout( );
		groupGridLayout.numColumns = 5;
		GridData groupGridData = new GridData( GridData.FILL_HORIZONTAL );

		Group dataFetchSettingGroup = new Group( composite, SWT.NONE );
		dataFetchSettingGroup.setLayoutData( groupGridData );
		dataFetchSettingGroup.setLayout( groupGridLayout );
		dataFetchSettingGroup.setText( Messages.getString( "dataset.editor.settings.dataFetchSetting" ) ); //$NON-NLS-1$

		fetchAllDataCheckBox = new Button( dataFetchSettingGroup, SWT.CHECK );
		GridData data = new GridData( );
		data.horizontalSpan = 5;
		fetchAllDataCheckBox.setLayoutData( data );
		fetchAllDataCheckBox.setText( Messages.getString( "dataset.editor.settings.dataFetchSetting.fetchAll" ) ); //$NON-NLS-1$

		final Label dataFetchLabel = new Label( dataFetchSettingGroup, SWT.NONE );
		dataFetchLabel.setText( Messages.getString( "SettingsPage.dataFetchSetting.label" ) ); //$NON-NLS-1$

		final Text rowFetchLimitText = new Text( dataFetchSettingGroup,
				SWT.BORDER );
		GridData gData = new GridData( GridData.FILL_HORIZONTAL);
		rowFetchLimitText.setLayoutData( gData );
		
		if ( getDataSetRowFetchLimit( ) > 0 )
		{
			fetchAllDataCheckBox.setSelection( false );
			rowFetchLimitText.setEnabled( true );
			dataFetchLabel.setEnabled( true );
			rowFetchLimitText.setText( Integer.toString( getDataSetRowFetchLimit( ) ) );
		}
		else
		{
			fetchAllDataCheckBox.setSelection( true );
			rowFetchLimitText.setEnabled( false );
			dataFetchLabel.setEnabled( false );
			rowFetchLimitText.setText( "" ); //$NON-NLS-1$

		}

		fetchAllDataCheckBox.addSelectionListener( new SelectionAdapter( ) {

			public void widgetSelected( SelectionEvent e )
			{

				final boolean isSelection = fetchAllDataCheckBox.getSelection( );
				dataFetchLabel.setEnabled( !isSelection );
				rowFetchLimitText.setEnabled( !isSelection );

				if ( isSelection )
				{
					rowFetchLimitText.setText( "" );  //$NON-NLS-1$
				}

			}
		} );

		rowFetchLimitText.addModifyListener( new ModifyListener( ) {

			public void modifyText( ModifyEvent e )
			{
				int rowFetchLimit = 0;

				try
				{

					if ( isNumber( rowFetchLimitText.getText( ) ) )
					{
						String rowLimitText = rowFetchLimitText.getText( );
						if ( rowLimitText.trim( ).length( ) == 0 )
							rowLimitText = "0"; //$NON-NLS-1$
						rowFetchLimit = new Double( rowLimitText ).intValue( );
						rowFetchLimit = rowFetchLimit < 0 ? 0 : rowFetchLimit;

						setDataSetRowFetchLimit( rowFetchLimit );

						getContainer( ).setMessage( DEFAULT_MESSAGE,
								IMessageProvider.NONE );
					}
					else
					{
						getContainer( ).setMessage( Messages.getString( "dataset.editor.settings.dataFetchSetting.errorNumberFormat" ), //$NON-NLS-1$
								IMessageProvider.ERROR );
					}

				}
				catch ( SemanticException e1 )
				{
					getContainer( ).setMessage( Messages.getString( "dataset.editor.settings.dataFetchSetting.errorNumberFormat" ), //$NON-NLS-1$
							IMessageProvider.ERROR );
				}

			}

		} );

	}

	private void addResultSetGroup( Composite composite )
	{
		GridLayout groupGridLayout = new GridLayout( );
		GridData groupGridData = new GridData( GridData.FILL_HORIZONTAL );

		Group resultSetNumberGroup = new Group( composite, SWT.NONE );
		resultSetNumberGroup.setLayoutData( groupGridData );
		resultSetNumberGroup.setLayout( groupGridLayout );
		resultSetNumberGroup.setText( Messages.getString( "dataset.editor.settings.resultsetselection.resultSetSelection" ) ); //$NON-NLS-1$

		selectResultSetCheckBox = new Button( resultSetNumberGroup, SWT.CHECK );
		GridData data = new GridData( );
		selectResultSetCheckBox.setLayoutData( data );
		selectResultSetCheckBox.setText( Messages.getString( "dataset.editor.settings.resultsetselection.enableResultSetSelection" ) ); //$NON-NLS-1$

		Composite selectionComposite = new Composite( resultSetNumberGroup, SWT.NONE );
		GridLayout cmpLayout = new GridLayout( );
		cmpLayout.numColumns = 5;
		selectionComposite.setLayout( cmpLayout );
		GridData cmpGridData = new GridData( GridData.FILL_HORIZONTAL );
		selectionComposite.setLayoutData( cmpGridData );
		
		resultSetName = new Button( selectionComposite, SWT.RADIO );
		data = new GridData( );
		data.horizontalSpan = 3;
		resultSetName.setLayoutData( data );
		resultSetName.setText( Messages.getString( "dataset.editor.settings.resultsetselection.selectResultSetByName" ) ); //$NON-NLS-1$

		final Text nameText = new Text( selectionComposite,
				SWT.BORDER );
		GridData gData = new GridData(GridData.FILL_HORIZONTAL );
		/*gData.horizontalSpan = 2;
		gData.widthHint = 100;*/
		nameText.setLayoutData( gData );

		resultSetNumber = new Button( selectionComposite, SWT.RADIO );
		data = new GridData( );
		data.horizontalSpan = 3;
		resultSetNumber.setLayoutData( data );
		resultSetNumber.setText( Messages.getString( "dataset.editor.settings.resultsetselection.selectResultSetByNumber" ) ); //$NON-NLS-1$

		final Text numberText = new Text( selectionComposite,
				SWT.BORDER );
		gData = new GridData( GridData.FILL_HORIZONTAL);
		numberText.setLayoutData( gData );


		selectResultSetCheckBox.addSelectionListener( new SelectionAdapter( ) {

			public void widgetSelected( SelectionEvent e )
			{
				final boolean selected = selectResultSetCheckBox.getSelection();
				resultSetName.setEnabled( selected );
				resultSetNumber.setEnabled( selected );
				
				if( selected )
				{
					if( resultSetName.getSelection())
					{
						numberText.setEnabled( false );
						nameText.setEnabled( true );
					}
					else if ( resultSetNumber.getSelection())
					{
						nameText.setEnabled( false );
						numberText.setEnabled( true );
					}
					else
					{
						nameText.setEnabled( selected );
						numberText.setEnabled( selected );
					}
				}
				else
				{
					nameText.setEnabled( selected );
					numberText.setEnabled( selected );
					
				}
				changed = true;
				
			}
		} );

		resultSetName.addSelectionListener( new SelectionAdapter( ) {

			public void widgetSelected( SelectionEvent e )
			{
				nameText.setEnabled( true );
				numberText.setEnabled( false );
				changed = true;
			}
		} );

		resultSetNumber.addSelectionListener( new SelectionAdapter( ) {

			public void widgetSelected( SelectionEvent e )
			{
				nameText.setEnabled( false );
				numberText.setEnabled( true );
				changed = true;
			}
		} );

		nameText.addModifyListener( new ModifyListener( ) {

			public void modifyText( ModifyEvent e )
			{
				DataSetSettingsPage.this.nameText = nameText.getText();
				changed = true;
			}

		} );

		numberText.addModifyListener( new ModifyListener( ) {

			public void modifyText( ModifyEvent e )
			{
				int rsNumber = 0;

				if ( isNumber( numberText.getText( ) ) )
				{
					String number = numberText.getText( );
					if ( number.trim( ).length( ) == 0 ) 
						number = "0"; //$NON-NLS-1$
					rsNumber = new Double( number ).intValue( );
					rsNumber = rsNumber < 0 ? 0 : rsNumber;
					DataSetSettingsPage.this.numberText = numberText.getText( );
					getContainer( ).setMessage( DEFAULT_MESSAGE,
							IMessageProvider.NONE );
					changed = true;
				}
				else
				{
					getContainer( ).setMessage( Messages.getString( "dataset.editor.settings.dataFetchSetting.errorNumberFormatForResultSet" ), //$NON-NLS-1$
							IMessageProvider.ERROR );
				}
			}

		} );
		
		if ( ( (OdaDataSetHandle) ( (DataSetEditor) getContainer( ) ).getHandle( ) ).getResultSetName( ) != null )
		{
			resultSetName.setSelection( true );
			nameText.setText( ( (OdaDataSetHandle) ( (DataSetEditor) getContainer( ) ).getHandle( ) ).getResultSetName( ) );
			numberText.setEnabled( false );
			selectResultSetCheckBox.setSelection( true );
		}
		else if ( ( (OdaDataSetHandle) ( (DataSetEditor) getContainer( ) ).getHandle( ) ).getPropertyHandle( IOdaDataSetModel.RESULT_SET_NUMBER_PROP )
				.isSet( ) )
		{
			resultSetNumber.setSelection( true );
			numberText.setText( String.valueOf( ( (OdaDataSetHandle) ( (DataSetEditor) getContainer( ) ).getHandle( ) ).getResultSetNumber( ) ) );
			nameText.setEnabled( false );
			selectResultSetCheckBox.setSelection( true );
		}
		else
		{
			selectResultSetCheckBox.setSelection( false );
			resultSetName.setSelection( true );
			resultSetName.setEnabled( false );
			resultSetNumber.setEnabled( false );
			nameText.setEnabled( false );
			numberText.setEnabled( false );
		}
	}
	/**
	 * Test the text to see if it can be parsed to an integer.
	 * 
	 * @param text
	 * @return
	 */
	private boolean isNumber( String text )
	{
		if ( text == null )
		{
			return false;
		}
		if ( text.trim( ).length( ) == 0 )
		{
			return true;
		}
		return text.matches( "^[0-9]*[1-9][0-9]*$" ); //$NON-NLS-1$
		
	}
	
	/**
	 * 
	 * @param count
	 * @throws SemanticException
	 */
	private void setDataSetRowFetchLimit( int count ) throws SemanticException
	{
		( (DataSetEditor) getContainer( ) ).getHandle( )
				.setRowFetchLimit( count );
	}

	/**
	 * 
	 * @return
	 */
	private int getDataSetRowFetchLimit( )
	{
		return ( (DataSetEditor) getContainer( ) ).getHandle( )
				.getRowFetchLimit( );

	}

	public void pageActivated( )
	{
		getContainer( ).setMessage( DEFAULT_MESSAGE, IMessageProvider.NONE );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.dialogs.properties.AbstractPropertyPage#performCancel()
	 */
	public boolean performCancel( )
	{
		// selectorImage.dispose( );
		return super.performCancel( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.dialogs.properties.AbstractPropertyPage#canLeave()
	 */
	public boolean canLeave( )
	{
		try
		{
			( (OdaDataSetHandle) ( (DataSetEditor) getContainer( ) ).getHandle( ) ).setProperty( IOdaDataSetModel.RESULT_SET_NUMBER_PROP,
					null );
			( (OdaDataSetHandle) ( (DataSetEditor) getContainer( ) ).getHandle( ) ).setResultSetName( null );
			
			if ( !updateResultSetSetting( ) )
				return true;
			
			return canLeavePage( );
		}
		catch ( Exception e )
		{
			return true;
		}
	}

	protected boolean canLeavePage( )
	{
		if ( canFinish( ) )
			return super.performOk( );
		else
			return false;
	}

	protected boolean updateResultSetSetting( ) throws SemanticException
	{
		if ( selectResultSetCheckBox == null )
			return false;

		if ( selectResultSetCheckBox.getSelection( ) )
		{
			if ( resultSetNumber.getSelection( ) )
			{
				( (OdaDataSetHandle) ( (DataSetEditor) getContainer( ) ).getHandle( ) ).setResultSetNumber( new Integer( this.numberText ) );
			}
			else if ( resultSetName.getSelection( ) )
			{
				( (OdaDataSetHandle) ( (DataSetEditor) getContainer( ) ).getHandle( ) ).setResultSetName( this.nameText );
			}
		}

		if ( changed )
		{
			Iterator it = ( (DataSetEditor) getContainer( ) ).getHandle( )
					.resultSetIterator( );
			while ( it.hasNext( ) )
			{
				it.remove( );
			}
		}
		changed = false;
		return true;
	}

	/**
	 * whether the page can be finished or leave.
	 */
	public boolean canFinish( )
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.dialogs.properties.AbstractPropertyPage#performOk()
	 */
	public boolean performOk( )
	{
		if ( canLeave( ) )
		{
			return super.performOk( );
		}
		else
			return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.data.ui.property.AbstractPropertyPage#getToolTip()
	 */
	public String getToolTip( )
	{
		return Messages.getString( "SettingsPage.CachePreference.Filter.Tooltip" ); //$NON-NLS-1$
	}

}
