package com.example.wifiautomanager.ui.rules;

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
public final class RuleListViewModel_Factory implements Factory<RuleListViewModel> {
  @Override
  public RuleListViewModel get() {
    return newInstance();
  }

  public static RuleListViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RuleListViewModel newInstance() {
    return new RuleListViewModel();
  }

  private static final class InstanceHolder {
    private static final RuleListViewModel_Factory INSTANCE = new RuleListViewModel_Factory();
  }
}
