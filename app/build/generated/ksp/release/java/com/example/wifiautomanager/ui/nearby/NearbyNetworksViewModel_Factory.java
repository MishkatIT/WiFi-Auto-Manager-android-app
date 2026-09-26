package com.example.wifiautomanager.ui.nearby;

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
public final class NearbyNetworksViewModel_Factory implements Factory<NearbyNetworksViewModel> {
  @Override
  public NearbyNetworksViewModel get() {
    return newInstance();
  }

  public static NearbyNetworksViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static NearbyNetworksViewModel newInstance() {
    return new NearbyNetworksViewModel();
  }

  private static final class InstanceHolder {
    private static final NearbyNetworksViewModel_Factory INSTANCE = new NearbyNetworksViewModel_Factory();
  }
}
