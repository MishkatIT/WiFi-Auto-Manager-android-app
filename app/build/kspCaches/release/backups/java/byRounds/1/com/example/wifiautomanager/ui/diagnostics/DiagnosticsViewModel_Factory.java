package com.example.wifiautomanager.ui.diagnostics;

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
public final class DiagnosticsViewModel_Factory implements Factory<DiagnosticsViewModel> {
  @Override
  public DiagnosticsViewModel get() {
    return newInstance();
  }

  public static DiagnosticsViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DiagnosticsViewModel newInstance() {
    return new DiagnosticsViewModel();
  }

  private static final class InstanceHolder {
    private static final DiagnosticsViewModel_Factory INSTANCE = new DiagnosticsViewModel_Factory();
  }
}
