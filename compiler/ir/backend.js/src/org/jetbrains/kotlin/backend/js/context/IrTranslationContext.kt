/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.js.context

import org.jetbrains.kotlin.backend.js.util.pureFqn
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class IrTranslationContext(val config: IrTranslationConfig, val fragment: JsProgramFragment) {
    private var mutableStatements: MutableCollection<JsStatement> = mutableListOf()
    private var mutableScope = config.scope
    private val nameSuggestion = NameSuggestion()
    private var currentDeclarationMutable: MemberDescriptor? = null
    private val aliasesImpl = mutableMapOf<DeclarationDescriptor, JsExpression>()
    val naming: NamingContext = NamingContextImpl(config.module.descriptor, nameSuggestion, config.scope, fragment)

    val statements: Collection<JsStatement> get() = mutableStatements
    val scope: JsScope get() = mutableScope
    val currentDeclaration: MemberDescriptor? = currentDeclarationMutable
    val module get() = config.module

    val aliases = object : Provider<DeclarationDescriptor, JsExpression?> {
        override fun get(key: DeclarationDescriptor): JsExpression? = aliasesImpl[key]
    }

    fun addStatements(statements: Collection<JsStatement>) {
        mutableStatements.addAll(statements)
    }

    fun addStatement(statement: JsStatement) {
        mutableStatements.add(statement)
    }

    fun <T> withStatements(statements: MutableCollection<JsStatement>, action: () -> T): T {
        val oldStatements = mutableStatements
        mutableStatements = statements
        val result = action()
        mutableStatements = oldStatements
        return result
    }

    fun <T> nestedDeclaration(declaration: MemberDescriptor, action: () -> T): T {
        val oldDeclaration = currentDeclarationMutable
        currentDeclarationMutable = declaration
        val result = action()
        currentDeclarationMutable = oldDeclaration
        return result
    }

    fun <T> withAliases(aliases: Collection<Pair<DeclarationDescriptor, JsExpression>>, action: () -> T): T {
        val backup = aliases.map { (descriptor, alias) ->
            val oldAlias = aliasesImpl[descriptor]
            aliasesImpl[descriptor] = alias
            Pair(descriptor, oldAlias)
        }
        val result = action()
        for ((descriptor, alias) in backup) {
            if (alias != null) {
                aliasesImpl[descriptor] = alias
            }
            else {
                aliasesImpl.remove(descriptor)
            }
        }
        return result
    }

    val declarationStatements: MutableList<JsStatement> get() = fragment.declarationBlock.statements

    val isPublicInlineFunction: Boolean
        get() {
            var descriptor: DeclarationDescriptor? = currentDeclaration
            while (descriptor is FunctionDescriptor) {
                if (descriptor.isInline && descriptor.isEffectivelyPublicApi) {
                    return true
                }
                descriptor = descriptor.containingDeclaration
            }
            return false
        }

    fun getInnerReference(descriptor: DeclarationDescriptor): JsExpression = pureFqn(naming.innerNames[descriptor])
}