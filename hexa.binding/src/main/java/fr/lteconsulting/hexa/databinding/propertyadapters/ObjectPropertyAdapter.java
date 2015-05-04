package fr.lteconsulting.hexa.databinding.propertyadapters;

import fr.lteconsulting.hexa.client.tools.Action2;
import fr.lteconsulting.hexa.databinding.Properties;
import fr.lteconsulting.hexa.databinding.ObjectPropertiesUtils;
import fr.lteconsulting.hexa.databinding.PropertyChangedEvent;
import fr.lteconsulting.hexa.databinding.PropertyChangedHandler;

/**
 * A PropertyAdapter implementation which is able to work with an object's field or property
 * 
 * To access the object's property, first the adapter tries to find a getter/setter. Then, if no
 * access method is found, the adapter works with the object's field value directly.
 * 
 * @author Arnaud
 *
 */
public class ObjectPropertyAdapter implements PropertyAdapter, PropertyChangedHandler
{
	private final Object source;
	private final String sourceProperty;

	private Action2<PropertyAdapter, Object> callback;
	private Object cookie;

	public ObjectPropertyAdapter( Object source, String sourceProperty )
	{
		this.source = source;
		this.sourceProperty = sourceProperty;
	}

	@Override
	public Object registerPropertyChanged( Action2<PropertyAdapter, Object> callback, Object cookie )
	{
		if( source == null )
			return null;
		
		this.callback = callback;
		this.cookie = cookie;
		
		return Properties.registerPropertyChangedEvent( source, sourceProperty, this );
	}

	@Override
	public void removePropertyChangedHandler( Object registration )
	{
		Properties.removePropertyChangedHandler( registration );
	}

	@Override
	public Object getValue()
	{
		return ObjectPropertiesUtils.GetProperty( source, sourceProperty, true );
	}

	@Override
	public void setValue( Object value )
	{
		ObjectPropertiesUtils.SetProperty( source, sourceProperty, value, true );
	}

	@Override
	public void onPropertyChanged( PropertyChangedEvent event )
	{
		if( callback == null )
			return;

		callback.exec( this, cookie );
	}
}