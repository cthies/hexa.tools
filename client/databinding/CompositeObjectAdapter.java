package com.hexa.client.databinding;

import com.hexa.client.classinfo.ClassInfo;
import com.hexa.client.classinfo.Clazz;
import com.hexa.client.classinfo.Field;
import com.hexa.client.classinfo.Method;
import com.hexa.client.databinding.DataBinding.DataAdapter;
import com.hexa.client.tools.Action1;

public class CompositeObjectAdapter implements DataAdapter
{
	private final Object source;
	private final String[] path;
	
	public CompositeObjectAdapter( Object source, String property )
	{
		this.source = source;
		path = property.split( "\\." );
	}
	
	class PropertyChangedManager implements Action1<DataBinding.DataAdapter>
	{
		PropertyChangedManager[] managers;
		Action1<DataAdapter> callback;
		
		String[] path;
		int position;
		
		ObjectAdapter adapter;
		Object handler;
		
		public PropertyChangedManager( String[] path, int position, Action1<DataAdapter> callback, PropertyChangedManager[] managers )
		{
			this.callback = callback;
			this.path = path;
			this.position = position;
			this.managers = managers;
		}
		
		public void register( Object source )
		{
			unregister();
			
			adapter = new ObjectAdapter( source, path[position] );
			
			handler = adapter.registerPropertyChanged( this );
			
			//handler = adapter.registerPropertyChanged( new PropertyChangedManager( path, i, adapters, handlers, managers ) );
		}
		
		public void unregister()
		{
			if( adapter != null && handler != null )
				adapter.removePropertyChangedHandler( handler );
			
			adapter = null;
			handler = null;
		}
		
		// a change occurs on the watched property
		@Override
		public void exec( DataAdapter param )
		{
			// is that the end ?
			if( position == path.length-1 )
			{
				callback.exec( CompositeObjectAdapter.this );
				return; // that's it
			}
			
			// get the current value, if not null, register on it
			Object currentValue = getValue( position );
			if( currentValue == null )
			{
				for( int r=position+1; r<managers.length; r++ )
					if( managers[r] != null )
						managers[r].unregister();
				return;
			}
			
			if( managers[position+1] == null )
				managers[position+1] = new PropertyChangedManager( path, position+1, callback, managers );
			
			managers[position+1].register( currentValue );
			
			managers[position+1].exec( param );
		}
	}
	
	@Override
	public Object registerPropertyChanged( final Action1<DataAdapter> callback )
	{
		PropertyChangedManager[] managers = new PropertyChangedManager[path.length];
		
		managers[0] = new PropertyChangedManager( path, 0, callback, managers );
		managers[0].register( source );
		
		return managers;
	}

	@Override
	public void removePropertyChangedHandler( Object handler )
	{
		PropertyChangedManager[] managers = (PropertyChangedManager[]) handler;
		for( int i=0; i<managers.length; i++ )
		{
			managers[i].unregister();
			managers[i] = null;
		}
	}
	
	private Object getValue( int level )
	{
		Object cur = source;
		
		for( int i=0; i<=level; i++)
		{
			if( cur == null )
				return null;
			
			Clazz<?> s = ClassInfo.Clazz( cur.getClass() );

			String getterName = "get" + canon(path[i]);
			Method getter = s.getMethod( getterName );
			if( getter != null )
			{
				cur = getter.call( cur, null );
			}
			else
			{
				//GWT.log( "ObjectAdapter : getter " + getterName + " not found, trying direct field access..." );
				
				// try direct field access
				Field<?> field = s.getField( path[i] );
				if( field != null )
				{
					cur = field.getValue( cur );
				}
				else
				{
					assert false : "ObjectAdapter : getter " + getterName + " and field not found !";
					return null;
				}
			}
		}
		
		return cur;
	}

	@Override
	public Object getValue()
	{
		return getValue(path.length-1);
	}

	@Override
	public void setValue( Object object )
	{
		Object cur = source;
		
		if( path.length > 1 )
			cur = getValue( path.length - 2 );
		
		if( cur == null )
		{
			assert false : "ObjectAdapter : Cannot find nothing to set !";
			return;
		}
		
		Clazz<?> s = ClassInfo.Clazz( cur.getClass() );

		String setterName = "set" + canon(path[path.length-1]);
		Method setter = s.getMethod( setterName );
		if( setter != null )
		{
			setter.call( cur, new Object[] { object } );
		}
		else
		{
			// try direct field access
			//GWT.log( "ObjectAdapter : setter " + setterName + " not found on object of class " + cur.getClass().getName() + " trying direct field access" );
			Field<?> field = s.getField( path[path.length-1] );
			if( field != null )
			{
				field.setValue( cur, object );
			}
			else
			{
				assert false : "ObjectAdapter : no setter nor field " + path[path.length-1] + " found on instance of class " + cur.getClass().getName();
			}
		}
	}
	
	private static String canon(String s)
	{
		return s.substring( 0, 1 ).toUpperCase() + s.substring( 1 );
	}
}
