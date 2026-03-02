/*
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogMessageBinding

class MessageDialog : FullscreenDialog() {

    override val gravity = Gravity.CENTER
    override val dimBackground = false

    private lateinit var binding: DialogMessageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_message, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val message = requireArguments().getString(ARG_MESSAGE)!!

        super.onViewCreated(view, savedInstanceState)
        binding.editTextValue.setText(message)
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_TAG = "arg_tag"

        private fun newInstance(message: String, tag: String = "dialog"): MessageDialog {
            return MessageDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                    putString(ARG_TAG, tag)
                }
            }
        }

        fun show(fragmentManager: FragmentManager, message: String, tag: String = "dialog") {
            if (fragmentManager.isStateSaved) return
            if (fragmentManager.findFragmentByTag(tag) != null) return

            newInstance(message).show(fragmentManager, tag)
        }
    }
}