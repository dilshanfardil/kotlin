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

package org.jetbrains.kotlin.backend.js.expression

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.js.backend.ast.*

class IrExpressionTranslationVisitor(private val context: IrTranslationContext) : IrElementVisitor<TypedExpression?, Unit> {
    override fun visitElement(element: IrElement, data: Unit): TypedExpression? = null

    override fun visitCall(expression: IrCall, data: Unit): TypedExpression {
        val function = expression.descriptor
        val dispatchReceiver = expression.dispatchReceiver?.accept(this, Unit)
        val extensionReceiver = expression.extensionReceiver?.accept(this, Unit)
        val arguments = function.valueParameters.indices.asSequence()
                .map { it to expression.getValueArgument(it) }
                .map { (index, expression) ->
                    val parameterType = function.valueParameters[index].type
                    val result = expression?.accept(this, Unit) ?: TypedExpression(
                            JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(0)), parameterType)
                    context.coerce(result, parameterType)
                }
                .toList()

        val intrinsic = context.config.intrinsics[function]
        if (intrinsic != null) {
            return intrinsic.apply(context, expression, dispatchReceiver, extensionReceiver, arguments)
        }

        val qualifier = if (dispatchReceiver != null) {
            JsNameRef(context.naming.names[function], dispatchReceiver.expression)
        }
        else {
            context.translateAsValueReference(function)
        }

        val allArguments = if (extensionReceiver != null) listOf(extensionReceiver) + arguments else arguments
        return TypedExpression(JsInvocation(qualifier, allArguments.map { it.expression }), expression.type)
    }

    override fun visitBlock(expression: IrBlock, data: Unit): TypedExpression? {
        val jsBlock = JsBlock()
        context.addStatement(jsBlock)
        return context.withStatements(jsBlock.statements) {
            for (statement in expression.statements.dropLast(1)) {
                statement.accept(this, Unit)?.let { (expression) ->
                    context.addStatement(JsExpressionStatement(expression))
                }
            }
            expression.statements.last().accept(this, Unit)
        }
    }

    override fun visitVariable(declaration: IrVariable, data: Unit): TypedExpression? {
        val property = declaration.descriptor
        val name = context.naming.names[property]
        val initializer = declaration.initializer?.accept(this, Unit)
                ?.let { context.coerce(it, property.type) }
        context.addStatement(JsVars(JsVars.JsVar(name, initializer?.expression)))
        return null
    }

    override fun visitGetValue(expression: IrGetValue, data: Unit): TypedExpression =
            TypedExpression(context.translateAsValueReference(expression.descriptor), expression.type)

    override fun visitWhileLoop(loop: IrWhileLoop, data: Unit): TypedExpression? {
        val conditionStatements = mutableListOf<JsStatement>()
        val condition = context.withStatements(conditionStatements) {
            context.coerce(loop.condition.accept(this, Unit)!!, context.module.irBuiltins.bool)
        }

        val bodyStatements = mutableListOf<JsStatement>()
        context.withStatements(bodyStatements) {
            loop.body?.accept(this, Unit)
        }

        val whileStatement = if (conditionStatements.isEmpty()) {
            val body = when (bodyStatements.size) {
                0 -> JsEmpty
                1 -> bodyStatements[0]
                else -> JsBlock(bodyStatements)
            }
            JsWhile(condition.expression, body)
        }
        else {
            val body = JsBlock()
            body.statements += conditionStatements
            body.statements += JsIf(JsPrefixOperation(JsUnaryOperator.NOT, condition.expression), JsBreak())
            body.statements += bodyStatements
            JsWhile(JsBooleanLiteral(true), body)
        }

        context.addStatement(whileStatement)

        return null
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Unit): TypedExpression? {
        val value = expression.value
        return when (value) {
            is String -> TypedExpression(JsStringLiteral(value), expression.type)
            is Int -> TypedExpression(JsIntLiteral(value), expression.type)
            else -> null
        }
    }
}