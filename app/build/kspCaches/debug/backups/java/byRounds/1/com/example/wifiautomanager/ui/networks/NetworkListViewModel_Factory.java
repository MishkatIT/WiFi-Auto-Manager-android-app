package com.example.wifiautomanager.ui.networks;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class NetworkListViewModel_Factory implements Factory<NetworkListViewModel> {
  @Override
  public NetworkListViewModel get() {
    return newInstance();
  }

  public static NetworkListViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static NetworkListViewModel newInstance() {
    return new NetworkListViewModel();
  }

  private static final class InstanceHolder {
    private static final NetworkListViewModel_Factory INSTANCE = new NetworkListViewModel_Factory();
  }
}
