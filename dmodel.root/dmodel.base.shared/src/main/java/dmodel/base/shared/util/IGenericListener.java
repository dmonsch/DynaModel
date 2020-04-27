package dmodel.base.shared.util;

@FunctionalInterface
public interface IGenericListener<T> {

	public void inform(T data);

}